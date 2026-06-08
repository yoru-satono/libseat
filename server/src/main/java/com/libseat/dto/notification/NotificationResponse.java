package com.libseat.dto.notification;

import com.libseat.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String content,
        UUID relatedId,
        boolean isRead,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
