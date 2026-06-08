package com.libseat.dto.auth;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
