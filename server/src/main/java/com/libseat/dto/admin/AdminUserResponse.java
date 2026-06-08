package com.libseat.dto.admin;

import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String userNo,
        String realName,
        String email,
        String phone,
        String department,
        UserRole role,
        UserStatus status,
        short failedLoginCount,
        short noShowCount,
        OffsetDateTime lockedUntil,
        OffsetDateTime suspendedUntil,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {}
