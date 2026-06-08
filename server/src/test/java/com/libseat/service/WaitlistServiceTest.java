package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.waitlist.JoinWaitlistRequest;
import com.libseat.dto.waitlist.WaitlistResponse;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTest {

    @Mock WaitlistRepository waitlistRepository;
    @Mock SeatRepository seatRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks WaitlistService waitlistService;

    private UUID userId, seatId, waitlistId;
    private User user;
    private Seat seat;

    @BeforeEach
    void setUp() {
        userId     = UUID.randomUUID();
        seatId     = UUID.randomUUID();
        waitlistId = UUID.randomUUID();

        Library lib = new Library();
        lib.setId(UUID.randomUUID()); lib.setName("总馆");

        user = new User();
        user.setId(userId); user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount((short) 0); user.setNoShowCount((short) 0);

        seat = new Seat();
        seat.setId(seatId); seat.setLibrary(lib);
        seat.setSeatNo("3F-001"); seat.setFloor((short) 3);
        seat.setStatus(SeatStatus.AVAILABLE);
    }

    // ── joinWaitlist ──────────────────────────────────────────────────────

    @Test
    void joinWaitlist_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(waitlistRepository.existsByUserIdAndSeatIdAndDateAndStartTimeAndEndTimeAndStatusIn(
                any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(waitlistRepository.save(any())).thenAnswer(i -> {
            Waitlist w = i.getArgument(0); w.setId(waitlistId); return w;
        });

        JoinWaitlistRequest req = buildRequest(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0));
        WaitlistResponse resp = waitlistService.joinWaitlist(userId, req);

        assertThat(resp.seatId()).isEqualTo(seatId);
        assertThat(resp.status()).isEqualTo(WaitlistStatus.WAITING);
        verify(waitlistRepository).save(any(Waitlist.class));
    }

    @Test
    void joinWaitlist_duplicate_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));
        when(waitlistRepository.existsByUserIdAndSeatIdAndDateAndStartTimeAndEndTimeAndStatusIn(
                any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> waitlistService.joinWaitlist(userId, buildRequest(
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_DUPLICATE);
    }

    @Test
    void joinWaitlist_pastDate_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(userId, buildRequest(
                LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_FORMAT_ERROR);
    }

    @Test
    void joinWaitlist_startAfterEnd_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(userId, buildRequest(
                LocalDate.now(), LocalTime.of(11, 0), LocalTime.of(9, 0))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARAM_FORMAT_ERROR);
    }

    @Test
    void joinWaitlist_suspendedUser_throws() {
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> waitlistService.joinWaitlist(userId, buildRequest(
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0))))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_SUSPENDED);
    }

    // ── listMyWaitlist ────────────────────────────────────────────────────

    @Test
    void listMyWaitlist_noFilter_returnsAll() {
        Waitlist w = waitingEntry();
        when(waitlistRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(w)));

        PageResult<WaitlistResponse> result = waitlistService.listMyWaitlist(userId, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        verify(waitlistRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), any());
    }

    @Test
    void listMyWaitlist_withStatusFilter_usesFilteredQuery() {
        Waitlist w = waitingEntry();
        when(waitlistRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(WaitlistStatus.WAITING), any()))
                .thenReturn(new PageImpl<>(List.of(w)));

        PageResult<WaitlistResponse> result = waitlistService.listMyWaitlist(userId, WaitlistStatus.WAITING, 1, 20);

        assertThat(result.items()).hasSize(1);
        verify(waitlistRepository).findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(userId), eq(WaitlistStatus.WAITING), any());
        verify(waitlistRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    // ── cancelWaitlist ────────────────────────────────────────────────────

    @Test
    void cancelWaitlist_success() {
        Waitlist w = waitingEntry();
        when(waitlistRepository.findById(waitlistId)).thenReturn(Optional.of(w));

        waitlistService.cancelWaitlist(userId, waitlistId);

        assertThat(w.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
    }

    @Test
    void cancelWaitlist_notFound_throws() {
        when(waitlistRepository.findById(waitlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitlistService.cancelWaitlist(userId, waitlistId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void cancelWaitlist_wrongUser_throws() {
        Waitlist w = waitingEntry();
        w.getUser().setId(UUID.randomUUID()); // 不同 user
        when(waitlistRepository.findById(waitlistId)).thenReturn(Optional.of(w));

        assertThatThrownBy(() -> waitlistService.cancelWaitlist(userId, waitlistId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void cancelWaitlist_alreadyExpired_throws() {
        Waitlist w = waitingEntry();
        w.setStatus(WaitlistStatus.EXPIRED);
        when(waitlistRepository.findById(waitlistId)).thenReturn(Optional.of(w));

        assertThatThrownBy(() -> waitlistService.cancelWaitlist(userId, waitlistId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_CANCELLABLE);
    }

    @Test
    void cancelWaitlist_alreadyConverted_throws() {
        Waitlist w = waitingEntry();
        w.setStatus(WaitlistStatus.CONVERTED);
        when(waitlistRepository.findById(waitlistId)).thenReturn(Optional.of(w));

        assertThatThrownBy(() -> waitlistService.cancelWaitlist(userId, waitlistId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_CANCELLABLE);
    }

    // ── notifyNextInQueue ─────────────────────────────────────────────────

    @Test
    void notifyNextInQueue_hasWaiting_notifiesFirst() {
        Waitlist w = waitingEntry();
        when(waitlistRepository.findWaitingQueue(seatId, LocalDate.now(),
                LocalTime.of(9, 0), LocalTime.of(11, 0))).thenReturn(List.of(w));

        waitlistService.notifyNextInQueue(seatId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThat(w.getStatus()).isEqualTo(WaitlistStatus.NOTIFIED);
        assertThat(w.getNotifiedAt()).isNotNull();
        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.WAITLIST_AVAILABLE
                && n.getUser().getId().equals(userId)));
    }

    @Test
    void notifyNextInQueue_emptyQueue_noOp() {
        when(waitlistRepository.findWaitingQueue(any(), any(), any(), any())).thenReturn(List.of());

        waitlistService.notifyNextInQueue(seatId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0));

        verify(notificationRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private JoinWaitlistRequest buildRequest(LocalDate date, LocalTime start, LocalTime end) {
        JoinWaitlistRequest req = new JoinWaitlistRequest();
        req.setSeatId(seatId); req.setDate(date);
        req.setStartTime(start); req.setEndTime(end);
        return req;
    }

    private Waitlist waitingEntry() {
        Waitlist w = new Waitlist();
        w.setId(waitlistId); w.setUser(user); w.setSeat(seat);
        w.setDate(LocalDate.now());
        w.setStartTime(LocalTime.of(9, 0)); w.setEndTime(LocalTime.of(11, 0));
        w.setStatus(WaitlistStatus.WAITING);
        w.setExpiresAt(OffsetDateTime.now().plusHours(2));
        return w;
    }
}
