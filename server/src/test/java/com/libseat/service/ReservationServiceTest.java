package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.reservation.CancelReservationRequest;
import com.libseat.dto.reservation.CheckInRequest;
import com.libseat.dto.reservation.CreateReservationRequest;
import com.libseat.dto.reservation.RenewRequest;
import com.libseat.dto.reservation.ReservationResponse;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import com.libseat.service.WaitlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock SeatRepository seatRepository;
    @Mock SystemRulesRepository systemRulesRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock WaitlistService waitlistService;
    @InjectMocks ReservationService reservationService;

    private User user;
    private Seat seat;
    private SystemRules rules;
    private UUID userId, seatId, reservationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        reservationId = UUID.randomUUID();

        Library lib = new Library(); lib.setId(UUID.randomUUID()); lib.setName("总馆");

        user = new User();
        user.setId(userId); user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.STUDENT);
        user.setFailedLoginCount((short) 0); user.setNoShowCount((short) 0);

        seat = new Seat();
        seat.setId(seatId); seat.setLibrary(lib);
        seat.setSeatNo("3F-001"); seat.setFloor((short) 3);
        seat.setStatus(SeatStatus.AVAILABLE); seat.setQrToken("q".repeat(64));

        rules = new SystemRules();
        rules.setOpenTimeStart(LocalTime.of(7, 0));
        rules.setOpenTimeEnd(LocalTime.of(22, 0));
        rules.setAdvanceDaysMax((short) 7);
        rules.setSingleMinMinutes((short) 30);
        rules.setSingleMaxHours((short) 4);
        rules.setDailyMaxHours((short) 8);
        rules.setCheckinEarlyMinutes((short) 10);
        rules.setCheckinLateMinutes((short) 15);
        rules.setNoShowThreshold((short) 3);
        rules.setSuspendDays((short) 7);
    }

    // ── createReservation ─────────────────────────────────────────────────

    @Test
    void createReservation_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));
        when(reservationRepository.findByUserIdAndDate(userId, LocalDate.now())).thenReturn(List.of());
        when(reservationRepository.findConflicting(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(seatId); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(9, 0)); req.setEndTime(LocalTime.of(11, 0));

        ReservationResponse resp = reservationService.createReservation(userId, req);
        assertThat(resp.seatId()).isEqualTo(seatId);
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.RESERVATION_SUCCESS
                && n.getUser().getId().equals(userId)));
    }

    @Test
    void createReservation_conflict_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));
        when(reservationRepository.findByUserIdAndDate(userId, LocalDate.now())).thenReturn(List.of());

        Reservation existing = new Reservation();
        when(reservationRepository.findConflicting(any(), any(), any(), any(), any()))
                .thenReturn(List.of(existing));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(seatId); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(9, 0)); req.setEndTime(LocalTime.of(11, 0));

        assertThatThrownBy(() -> reservationService.createReservation(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_CONFLICT);
    }

    @Test
    void createReservation_dailyLimitExceeded_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));

        // 已预约 7 小时（420 分钟），再预约 2 小时超出每日 8 小时上限
        Reservation existing = new Reservation();
        existing.setStatus(ReservationStatus.ACTIVE);
        existing.setStartTime(LocalTime.of(7, 0)); existing.setEndTime(LocalTime.of(14, 0));
        when(reservationRepository.findByUserIdAndDate(userId, LocalDate.now()))
                .thenReturn(List.of(existing));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(seatId); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(15, 0)); req.setEndTime(LocalTime.of(17, 0));

        assertThatThrownBy(() -> reservationService.createReservation(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void createReservation_durationTooShort_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(seatId); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(9, 0)); req.setEndTime(LocalTime.of(9, 15)); // 15 min < 30

        assertThatThrownBy(() -> reservationService.createReservation(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_DURATION_INVALID);
    }

    @Test
    void createReservation_outOfHours_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(seatId); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(6, 0)); req.setEndTime(LocalTime.of(8, 0)); // 开始时间 < 7:00

        assertThatThrownBy(() -> reservationService.createReservation(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_OUT_OF_HOURS);
    }

    // ── cancelReservation ─────────────────────────────────────────────────

    @Test
    void cancelReservation_success() {
        Reservation r = activeReservation(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        reservationService.cancelReservation(userId, reservationId, null);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(r.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelReservation_withReason_savesCancelReason() {
        Reservation r = activeReservation(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        CancelReservationRequest req = new CancelReservationRequest();
        req.setCancelReason("临时有事");
        reservationService.cancelReservation(userId, reservationId, req);

        assertThat(r.getCancelReason()).isEqualTo("临时有事");
    }

    @Test
    void cancelReservation_alreadyStarted_throws() {
        // 昨天的预约：时间已远超开始前30分钟
        Reservation r = activeReservation(LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservationService.cancelReservation(userId, reservationId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_CANCEL_TOO_LATE);
    }

    @Test
    void cancelReservation_withinThirtyMinutes_throws() {
        // 今天、10分钟后开始：在30分钟截止窗口内
        LocalTime start = LocalTime.now().plusMinutes(10).withSecond(0).withNano(0);
        Reservation r = activeReservation(LocalDate.now(), start, start.plusHours(2));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservationService.cancelReservation(userId, reservationId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_CANCEL_TOO_LATE);
    }

    @Test
    void cancelReservation_notActive_throws() {
        Reservation r = activeReservation(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        r.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservationService.cancelReservation(userId, reservationId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESERVATION_ALREADY_STARTED);
    }

    @Test
    void cancelReservation_wrongUser_throws() {
        Reservation r = activeReservation(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0));
        r.getUser().setId(UUID.randomUUID()); // 不同 userId
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> reservationService.cancelReservation(userId, reservationId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // ── checkIn ───────────────────────────────────────────────────────────

    @Test
    void checkIn_success() {
        // Use now as start time so the check-in window (start-10min to start+15min) always includes now
        LocalTime start = LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        Reservation r = activeReservation(LocalDate.now(), start, start.plusHours(1));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));

        CheckInRequest req = new CheckInRequest(); req.setQrToken("q".repeat(64));
        reservationService.checkIn(userId, reservationId, req);

        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(r.getCheckinAt()).isNotNull();
    }

    @Test
    void checkIn_wrongQrToken_throws() {
        Reservation r = activeReservation(LocalDate.now(), LocalTime.now().minusMinutes(5), LocalTime.now().plusHours(1));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        CheckInRequest req = new CheckInRequest(); req.setQrToken("wrong");
        assertThatThrownBy(() -> reservationService.checkIn(userId, reservationId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_INVALID);
    }

    @Test
    void checkIn_outsideWindow_throws() {
        // 预约开始时间 1 小时后，还未到签到窗口
        Reservation r = activeReservation(LocalDate.now(), LocalTime.now().plusHours(1), LocalTime.now().plusHours(3));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(systemRulesRepository.findByLibraryId(any())).thenReturn(Optional.of(rules));

        CheckInRequest req = new CheckInRequest(); req.setQrToken("q".repeat(64));
        assertThatThrownBy(() -> reservationService.checkIn(userId, reservationId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHECKIN_WINDOW_CLOSED);
    }

    // ── renew ─────────────────────────────────────────────────────────────

    @Test
    void renew_success() {
        LocalTime end = LocalTime.now().plusMinutes(10); // 还有 10 分钟结束，在续约窗口内
        Reservation r = checkedInReservation(LocalDate.now(), LocalTime.now().minusHours(1), end);
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));
        when(reservationRepository.findConflicting(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RenewRequest req = new RenewRequest(); req.setNewEndTime(end.plusHours(1));
        ReservationResponse resp = reservationService.renew(userId, reservationId, req);

        assertThat(resp).isNotNull();
    }

    @Test
    void renew_outsideWindow_throws() {
        // 距结束还有 30 分钟，不在续约窗口（< 15 分钟前）
        LocalTime end = LocalTime.now().plusMinutes(30);
        Reservation r = activeReservation(LocalDate.now(), LocalTime.now().minusHours(1), end);
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(r));

        RenewRequest req = new RenewRequest(); req.setNewEndTime(end.plusHours(1));
        assertThatThrownBy(() -> reservationService.renew(userId, reservationId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RENEW_WINDOW_CLOSED);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Reservation activeReservation(LocalDate date, LocalTime start, LocalTime end) {
        Reservation r = new Reservation();
        r.setId(reservationId); r.setUser(user); r.setSeat(seat);
        r.setDate(date); r.setStartTime(start); r.setEndTime(end);
        r.setStatus(ReservationStatus.ACTIVE);
        return r;
    }

    private Reservation checkedInReservation(LocalDate date, LocalTime start, LocalTime end) {
        Reservation r = activeReservation(date, start, end);
        r.setStatus(ReservationStatus.CHECKED_IN);
        r.setCheckinAt(OffsetDateTime.now());
        return r;
    }
}
