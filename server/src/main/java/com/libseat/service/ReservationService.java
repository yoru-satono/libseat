package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.reservation.*;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final SystemRulesRepository systemRulesRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final WaitlistService waitlistService;

    public PageResult<ReservationResponse> listMyReservations(
            UUID userId, ReservationStatus status,
            LocalDate dateFrom, LocalDate dateTo, int page, int pageSize) {
        Specification<Reservation> spec = Specification
                .where(ReservationSpecifications.forUser(userId))
                .and(ReservationSpecifications.withStatus(status))
                .and(ReservationSpecifications.fromDate(dateFrom))
                .and(ReservationSpecifications.toDate(dateTo));
        Page<Reservation> pg = reservationRepository.findAll(
                spec, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(
                pg.getContent().stream().map(ReservationService::toResponse).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    public List<ReservationResponse> listMyReservationsForExport(
            UUID userId, ReservationStatus status, LocalDate dateFrom, LocalDate dateTo) {
        Specification<Reservation> spec = Specification
                .where(ReservationSpecifications.forUser(userId))
                .and(ReservationSpecifications.withStatus(status))
                .and(ReservationSpecifications.fromDate(dateFrom))
                .and(ReservationSpecifications.toDate(dateTo));
        List<Reservation> rows = reservationRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (rows.size() > 5000) {
            throw new BusinessException(ErrorCode.EXPORT_LIMIT_EXCEEDED);
        }
        return rows.stream().map(ReservationService::toResponse).toList();
    }

    public ReservationResponse getReservation(UUID userId, UUID reservationId) {
        Reservation r = findReservation(reservationId);
        if (!r.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponse(r);
    }

    @Transactional
    public ReservationResponse createReservation(UUID userId, CreateReservationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        checkUserCanReserve(user);

        Seat seat = seatRepository.findById(req.getSeatId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "座位不存在"));
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE);
        }

        LocalDate date = req.getDate();
        LocalTime startTime = req.getStartTime();
        LocalTime endTime = req.getEndTime();
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "开始时间必须早于结束时间");
        }

        SystemRules rules = loadRules(seat.getLibrary().getId());

        validateAdvanceDays(date, rules);
        validateOpenHours(startTime, endTime, rules);
        validateDuration(startTime, endTime, rules);
        validateDailyLimit(userId, date, startTime, endTime, rules);

        List<Reservation> conflicts = reservationRepository.findConflicting(
                seat.getId(), date, startTime, endTime,
                EnumSet.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW));
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_CONFLICT);
        }

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setSeat(seat);
        reservation.setDate(date);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservationRepository.save(reservation);

        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(NotificationType.RESERVATION_SUCCESS);
        notif.setTitle("预约成功");
        notif.setContent(String.format("您已成功预约 %s %s %s–%s 的座位 %s。",
                seat.getLibrary().getName(), date, startTime, endTime, seat.getSeatNo()));
        notif.setRelatedId(reservation.getId());
        notificationRepository.save(notif);

        return toResponse(reservation);
    }

    @Transactional
    public void cancelReservation(UUID userId, UUID reservationId, CancelReservationRequest req) {
        Reservation r = findReservation(reservationId);
        if (!r.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_STARTED);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = LocalDateTime.of(r.getDate(), r.getStartTime());
        if (!now.isBefore(startDateTime.minusMinutes(30))) {
            throw new BusinessException(ErrorCode.RESERVATION_CANCEL_TOO_LATE);
        }

        r.setStatus(ReservationStatus.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        if (req != null && req.getCancelReason() != null) {
            r.setCancelReason(req.getCancelReason());
        }

        Notification notif = new Notification();
        notif.setUser(r.getUser());
        notif.setType(NotificationType.RESERVATION_CANCELLED);
        notif.setTitle("预约已取消");
        notif.setContent(String.format("您在 %s %s %s–%s 的座位 %s 预约已取消。",
                r.getSeat().getLibrary().getName(), r.getDate(),
                r.getStartTime(), r.getEndTime(), r.getSeat().getSeatNo()));
        notif.setRelatedId(r.getId());
        notificationRepository.save(notif);

        waitlistService.notifyNextInQueue(
                r.getSeat().getId(), r.getDate(), r.getStartTime(), r.getEndTime());
    }

    @Transactional
    public void checkIn(UUID userId, UUID reservationId, CheckInRequest req) {
        Reservation r = findReservation(reservationId);
        if (!r.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (r.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CHECKIN_WINDOW_CLOSED);
        }

        Seat seat = r.getSeat();
        if (!req.getQrToken().equals(seat.getQrToken())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "二维码无效");
        }

        SystemRules rules = loadRules(seat.getLibrary().getId());
        LocalDateTime startDt = LocalDateTime.of(r.getDate(), r.getStartTime());
        LocalDateTime windowStart = startDt.minusMinutes(rules.getCheckinEarlyMinutes());
        LocalDateTime windowEnd = startDt.plusMinutes(rules.getCheckinLateMinutes());
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            throw new BusinessException(ErrorCode.CHECKIN_WINDOW_CLOSED);
        }

        r.setStatus(ReservationStatus.CHECKED_IN);
        r.setCheckinAt(OffsetDateTime.now());
    }

    @Transactional
    public ReservationResponse renew(UUID userId, UUID reservationId, RenewRequest req) {
        Reservation current = findReservation(reservationId);
        if (!current.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!EnumSet.of(ReservationStatus.ACTIVE, ReservationStatus.CHECKED_IN, ReservationStatus.IN_USE)
                .contains(current.getStatus())) {
            throw new BusinessException(ErrorCode.RENEW_WINDOW_CLOSED);
        }

        LocalDateTime endDt = LocalDateTime.of(current.getDate(), current.getEndTime());
        LocalDateTime renewWindow = endDt.minusMinutes(15);
        if (LocalDateTime.now().isBefore(renewWindow)) {
            throw new BusinessException(ErrorCode.RENEW_WINDOW_CLOSED);
        }

        LocalTime newEndTime = req.getNewEndTime();
        if (!current.getEndTime().isBefore(newEndTime)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT_ERROR, "续约结束时间必须晚于当前结束时间");
        }

        List<Reservation> conflicts = reservationRepository.findConflicting(
                current.getSeat().getId(), current.getDate(),
                current.getEndTime(), newEndTime,
                EnumSet.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW));
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.RENEW_SEAT_OCCUPIED);
        }

        Reservation renewed = new Reservation();
        renewed.setUser(current.getUser());
        renewed.setSeat(current.getSeat());
        renewed.setDate(current.getDate());
        renewed.setStartTime(current.getEndTime());
        renewed.setEndTime(newEndTime);
        renewed.setStatus(ReservationStatus.ACTIVE);
        renewed.setParent(current);
        reservationRepository.save(renewed);

        Notification notif = new Notification();
        notif.setUser(current.getUser());
        notif.setType(NotificationType.RENEWAL_SUCCESS);
        notif.setTitle("续约成功");
        notif.setContent(String.format("您在 %s %s 的座位 %s 已续约至 %s。",
                current.getSeat().getLibrary().getName(), current.getDate(),
                current.getSeat().getSeatNo(), newEndTime));
        notif.setRelatedId(renewed.getId());
        notificationRepository.save(notif);

        return toResponse(renewed);
    }

    private void checkUserCanReserve(User user) {
        if (user.getStatus() == UserStatus.INACTIVE) throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        if (user.getStatus() == UserStatus.SUSPENDED) throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
    }

    private void validateAdvanceDays(LocalDate date, SystemRules rules) {
        long daysAhead = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (daysAhead < 0 || daysAhead > rules.getAdvanceDaysMax()) {
            throw new BusinessException(ErrorCode.RESERVATION_TOO_EARLY);
        }
    }

    private void validateOpenHours(LocalTime startTime, LocalTime endTime, SystemRules rules) {
        if (startTime.isBefore(rules.getOpenTimeStart()) || endTime.isAfter(rules.getOpenTimeEnd())) {
            throw new BusinessException(ErrorCode.RESERVATION_OUT_OF_HOURS);
        }
    }

    private void validateDuration(LocalTime startTime, LocalTime endTime, SystemRules rules) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < rules.getSingleMinMinutes()) {
            throw new BusinessException(ErrorCode.RESERVATION_DURATION_INVALID,
                    "预约时长不得少于 " + rules.getSingleMinMinutes() + " 分钟");
        }
        if (minutes > (long) rules.getSingleMaxHours() * 60) {
            throw new BusinessException(ErrorCode.RESERVATION_DURATION_INVALID,
                    "单次预约时长不得超过 " + rules.getSingleMaxHours() + " 小时");
        }
    }

    private void validateDailyLimit(UUID userId, LocalDate date,
                                    LocalTime startTime, LocalTime endTime, SystemRules rules) {
        List<Reservation> existing = reservationRepository.findByUserIdAndDate(userId, date);
        long usedMinutes = existing.stream()
                .filter(r -> EnumSet.of(ReservationStatus.ACTIVE,
                        ReservationStatus.CHECKED_IN, ReservationStatus.IN_USE).contains(r.getStatus()))
                .mapToLong(r -> Duration.between(r.getStartTime(), r.getEndTime()).toMinutes())
                .sum();
        long newMinutes = Duration.between(startTime, endTime).toMinutes();
        if (usedMinutes + newMinutes > (long) rules.getDailyMaxHours() * 60) {
            throw new BusinessException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
    }

    private SystemRules loadRules(UUID libraryId) {
        return systemRulesRepository.findByLibraryId(libraryId)
                .orElseGet(() -> systemRulesRepository.findByLibraryIsNull()
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "系统规则未配置")));
    }

    private Reservation findReservation(UUID reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "预约不存在"));
    }

    static ReservationResponse toResponse(Reservation r) {
        Seat seat = r.getSeat();
        return new ReservationResponse(
                r.getId(),
                seat.getId(),
                seat.getSeatNo(),
                seat.getLibrary().getName(),
                seat.getFloor(),
                seat.getArea(),
                r.getDate(),
                r.getStartTime(),
                r.getEndTime(),
                r.getStatus(),
                r.getCheckinAt(),
                r.getCancelledAt(),
                r.getCancelReason(),
                r.getCreatedAt());
    }
}
