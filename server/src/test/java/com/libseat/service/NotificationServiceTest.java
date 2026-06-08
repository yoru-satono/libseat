package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.notification.NotificationResponse;
import com.libseat.entity.*;
import com.libseat.exception.BusinessException;
import com.libseat.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationService notificationService;

    private UUID userId;
    private UUID notifId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notifId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
    }

    // ── listNotifications ─────────────────────────────────────────────────

    @Test
    void listNotifications_allNotifications_returnsPage() {
        Notification n = notification(userId, false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        PageResult<NotificationResponse> result =
                notificationService.listNotifications(userId, false, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).isRead()).isFalse();
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(userId), any());
    }

    @Test
    void listNotifications_unreadOnly_usesFilteredQuery() {
        Notification n = notification(userId, false);
        when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(eq(userId), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        PageResult<NotificationResponse> result =
                notificationService.listNotifications(userId, true, 1, 20);

        assertThat(result.items()).hasSize(1);
        verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(eq(userId), eq(false), any());
        verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    // ── markRead ──────────────────────────────────────────────────────────

    @Test
    void markRead_success() {
        Notification n = notification(userId, false);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        notificationService.markRead(userId, notifId);

        assertThat(n.getIsRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markRead_alreadyRead_noOp() {
        Notification n = notification(userId, true);
        n.setReadAt(OffsetDateTime.now().minusMinutes(1));
        OffsetDateTime originalReadAt = n.getReadAt();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        notificationService.markRead(userId, notifId);

        assertThat(n.getReadAt()).isEqualTo(originalReadAt); // 未被更新
    }

    @Test
    void markRead_notFound_throws() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(userId, notifId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void markRead_wrongUser_throws() {
        Notification n = notification(UUID.randomUUID(), false); // 不同 user
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markRead(userId, notifId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // ── markAllRead ───────────────────────────────────────────────────────

    @Test
    void markAllRead_delegatesToRepository() {
        notificationService.markAllRead(userId);
        verify(notificationRepository).markAllRead(eq(userId), any(OffsetDateTime.class));
    }

    // ── deleteNotification ────────────────────────────────────────────────

    @Test
    void deleteNotification_success() {
        Notification n = notification(userId, false);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        notificationService.deleteNotification(userId, notifId);

        verify(notificationRepository).delete(n);
    }

    @Test
    void deleteNotification_notFound_throws() {
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(userId, notifId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteNotification_wrongUser_throws() {
        Notification n = notification(UUID.randomUUID(), false);
        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.deleteNotification(userId, notifId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    // ── helper ────────────────────────────────────────────────────────────

    private Notification notification(UUID ownerId, boolean isRead) {
        User owner = new User();
        owner.setId(ownerId);
        Notification n = new Notification();
        n.setId(notifId);
        n.setUser(owner);
        n.setType(NotificationType.RESERVATION_SUCCESS);
        n.setTitle("预约成功");
        n.setContent("内容");
        n.setIsRead(isRead);
        if (isRead) n.setReadAt(OffsetDateTime.now());
        return n;
    }
}
