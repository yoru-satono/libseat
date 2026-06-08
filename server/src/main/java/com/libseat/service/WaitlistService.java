package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.waitlist.JoinWaitlistRequest;
import com.libseat.dto.waitlist.WaitlistResponse;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public WaitlistResponse joinWaitlist(UUID userId, JoinWaitlistRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        checkUserCanReserve(user);

        Seat seat = seatRepository.findById(req.getSeatId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "座位不存在"));

        LocalDate date = req.getDate();
        LocalTime startTime = req.getStartTime();
        LocalTime endTime = req.getEndTime();

        if (!date.isAfter(LocalDate.now().minusDays(1))) {
            // 过去的日期不受理（允许今天）
            if (date.isBefore(LocalDate.now())) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "不能对过去的日期加入等待队列");
            }
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "开始时间必须早于结束时间");
        }

        boolean alreadyWaiting = waitlistRepository
                .existsByUserIdAndSeatIdAndDateAndStartTimeAndEndTimeAndStatusIn(
                        userId, req.getSeatId(), date, startTime, endTime,
                        EnumSet.of(WaitlistStatus.WAITING, WaitlistStatus.NOTIFIED));
        if (alreadyWaiting) {
            throw new BusinessException(ErrorCode.WAITLIST_DUPLICATE);
        }

        Waitlist w = new Waitlist();
        w.setUser(user);
        w.setSeat(seat);
        w.setDate(date);
        w.setStartTime(startTime);
        w.setEndTime(endTime);
        w.setStatus(WaitlistStatus.WAITING);
        w.setExpiresAt(LocalDateTime.of(date, startTime)
                .atOffset(OffsetDateTime.now().getOffset()));
        waitlistRepository.save(w);

        return toResponse(w);
    }

    public PageResult<WaitlistResponse> listMyWaitlist(
            UUID userId, WaitlistStatus status, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        Page<Waitlist> pg = status != null
                ? waitlistRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : waitlistRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResult.of(
                pg.getContent().stream().map(WaitlistService::toResponse).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    @Transactional
    public void cancelWaitlist(UUID userId, UUID waitlistId) {
        Waitlist w = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!w.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!EnumSet.of(WaitlistStatus.WAITING, WaitlistStatus.NOTIFIED).contains(w.getStatus())) {
            throw new BusinessException(ErrorCode.WAITLIST_NOT_CANCELLABLE);
        }
        w.setStatus(WaitlistStatus.EXPIRED);
    }

    /**
     * 某时段预约释放（取消/爽约）时，通知等待队列中的第一位用户。
     * 由 ReservationService 和 ReservationScheduler 调用。
     */
    @Transactional
    public void notifyNextInQueue(UUID seatId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Waitlist> queue = waitlistRepository.findWaitingQueue(seatId, date, startTime, endTime);
        if (queue.isEmpty()) return;

        Waitlist first = queue.get(0);
        first.setStatus(WaitlistStatus.NOTIFIED);
        first.setNotifiedAt(OffsetDateTime.now());

        Seat seat = first.getSeat();
        Notification notif = new Notification();
        notif.setUser(first.getUser());
        notif.setType(NotificationType.WAITLIST_AVAILABLE);
        notif.setTitle("等待的座位已可预约");
        notif.setContent(String.format("您等待的座位 %s（%s %s %s–%s）现已可预约，请尽快完成预约。",
                seat.getSeatNo(), seat.getLibrary().getName(), date, startTime, endTime));
        notif.setRelatedId(first.getId());
        notificationRepository.save(notif);
    }

    private void checkUserCanReserve(User user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
    }

    private static WaitlistResponse toResponse(Waitlist w) {
        Seat seat = w.getSeat();
        return new WaitlistResponse(
                w.getId(), seat.getId(), seat.getSeatNo(), seat.getLibrary().getName(),
                seat.getFloor(), seat.getArea(), w.getDate(), w.getStartTime(), w.getEndTime(),
                w.getStatus(), w.getNotifiedAt(), w.getExpiresAt(), w.getCreatedAt());
    }
}
