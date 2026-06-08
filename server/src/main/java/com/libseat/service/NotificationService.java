package com.libseat.service;

import com.libseat.common.ErrorCode;
import com.libseat.dto.PageResult;
import com.libseat.dto.notification.NotificationResponse;
import com.libseat.entity.Notification;
import com.libseat.exception.BusinessException;
import com.libseat.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public PageResult<NotificationResponse> listNotifications(
            UUID userId, Boolean isRead, int page, int pageSize) {
        PageRequest pageable = PageRequest.of(page - 1, pageSize);
        Page<Notification> pg = (isRead != null)
                ? notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResult.of(
                pg.getContent().stream().map(NotificationService::toResponse).toList(),
                pg.getTotalElements(), page, pageSize);
    }

    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadAt(OffsetDateTime.now());
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, OffsetDateTime.now());
    }

    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notificationRepository.delete(n);
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getContent(),
                n.getRelatedId(), Boolean.TRUE.equals(n.getIsRead()), n.getReadAt(), n.getCreatedAt());
    }
}
