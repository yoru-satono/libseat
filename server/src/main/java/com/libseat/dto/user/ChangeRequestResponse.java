package com.libseat.dto.user;

import com.libseat.entity.ChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChangeRequestResponse(
        UUID id,
        String fieldName,
        String oldValue,
        String newValue,
        ChangeRequestStatus status,
        String handleNote,
        OffsetDateTime createdAt,
        OffsetDateTime handledAt
) {}
