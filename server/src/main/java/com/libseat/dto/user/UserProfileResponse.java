package com.libseat.dto.user;

import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String userNo,
        String realName,
        String email,
        String phone,
        String department,
        UserRole role,
        UserStatus status,
        short noShowCount,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {}
