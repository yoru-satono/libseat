package com.libseat.dto.admin;

import com.libseat.entity.ChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminChangeRequestResponse(
        UUID id,
        UUID userId,
        String userNo,
        String realName,
        String fieldName,
        String oldValue,
        String newValue,
        ChangeRequestStatus status,
        String handleNote,
        OffsetDateTime createdAt,
        OffsetDateTime handledAt
) {}
