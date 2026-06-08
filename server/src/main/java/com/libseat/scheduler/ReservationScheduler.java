package com.libseat.scheduler;

import com.libseat.entity.*;
import com.libseat.repository.EmailTokenRepository;
import com.libseat.repository.NotificationRepository;
import com.libseat.repository.ReservationRepository;
import com.libseat.repository.SystemRulesRepository;
import com.libseat.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final SystemRulesRepository systemRulesRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final NotificationRepository notificationRepository;
    private final WaitlistService waitlistService;

    /**
     * 每 5 分钟执行一次：
     * 1. 将签到窗口已关闭的 ACTIVE 预约标记为 NO_SHOW，累计用户爽约次数并按需暂停账号
     * 2. 将结束时间已过的 CHECKED_IN / IN_USE 预约标记为 COMPLETED
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void processExpiredReservations() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        SystemRules rules = systemRulesRepository.findByLibraryIsNull().orElse(null);
        int lateMinutes   = rules != null ? rules.getCheckinLateMinutes() : 15;
        short threshold   = rules != null ? rules.getNoShowThreshold()    : 3;
        short suspendDays = rules != null ? rules.getSuspendDays()        : 7;

        // ── 爽约标记 ──────────────────────────────────────────────────────
        List<Reservation> activeOld = reservationRepository
                .findByStatusAndDateLessThanEqual(ReservationStatus.ACTIVE, today);

        int noShowCount = 0;
        for (Reservation r : activeOld) {
            LocalDateTime windowEnd = LocalDateTime.of(r.getDate(), r.getStartTime())
                    .plusMinutes(lateMinutes);
            if (now.isAfter(windowEnd)) {
                r.setStatus(ReservationStatus.NO_SHOW);

                User user = r.getUser();
                short cnt = (short) (user.getNoShowCount() + 1);
                user.setNoShowCount(cnt);

                Notification noShowNotif = new Notification();
                noShowNotif.setUser(user);
                noShowNotif.setType(NotificationType.NO_SHOW_WARNING);
                noShowNotif.setTitle("爽约提醒");
                noShowNotif.setContent(String.format(
                        "您在 %s %s %s–%s 的预约未签到已被标记为爽约，当前累计爽约 %d 次。",
                        r.getSeat().getLibrary().getName(), r.getDate(),
                        r.getStartTime(), r.getEndTime(), cnt));
                noShowNotif.setRelatedId(r.getId());
                notificationRepository.save(noShowNotif);

                if (cnt >= threshold) {
                    user.setSuspendedUntil(OffsetDateTime.now().plusDays(suspendDays));
                    user.setNoShowCount((short) 0);
                    log.info("用户 {} 爽约次数达到阈值，暂停预约权限 {} 天", user.getId(), suspendDays);

                    Notification suspendNotif = new Notification();
                    suspendNotif.setUser(user);
                    suspendNotif.setType(NotificationType.ACCOUNT_SUSPENDED);
                    suspendNotif.setTitle("预约权限已暂停");
                    suspendNotif.setContent(String.format(
                            "您的累计爽约次数已达上限，预约权限已被暂停 %d 天。", suspendDays));
                    notificationRepository.save(suspendNotif);
                }
                waitlistService.notifyNextInQueue(
                        r.getSeat().getId(), r.getDate(), r.getStartTime(), r.getEndTime());
                noShowCount++;
            }
        }

        // ── 完成标记 ──────────────────────────────────────────────────────
        List<Reservation> inProgress = reservationRepository.findByStatusInAndDateLessThanEqual(
                EnumSet.of(ReservationStatus.CHECKED_IN, ReservationStatus.IN_USE), today);

        int completedCount = 0;
        for (Reservation r : inProgress) {
            LocalDateTime endDt = LocalDateTime.of(r.getDate(), r.getEndTime());
            if (now.isAfter(endDt)) {
                r.setStatus(ReservationStatus.COMPLETED);
                r.setCompletedAt(OffsetDateTime.now());
                completedCount++;
            }
        }

        if (noShowCount > 0 || completedCount > 0) {
            log.info("定时任务完成：标记爽约 {} 条，标记完成 {} 条", noShowCount, completedCount);
        }
    }

    /** 每天凌晨 3 点清理过期未使用的邮件令牌 */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        int deleted = emailTokenRepository.deleteExpired(OffsetDateTime.now());
        log.info("已清理过期邮件令牌 {} 条", deleted);
    }
}
