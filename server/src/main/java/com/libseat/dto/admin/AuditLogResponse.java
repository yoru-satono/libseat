package com.libseat.dto.admin;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        Long id,
        UUID adminId,
        String adminName,
        String actionType,
        String targetType,
        String targetId,
        Map<String, Object> detail,
        String ipAddress,
        OffsetDateTime createdAt
) {}
