package com.libseat.dto.waitlist;

import com.libseat.entity.SeatArea;
import com.libseat.entity.WaitlistStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WaitlistResponse(
        UUID id,
        UUID seatId,
        String seatNo,
        String libraryName,
        Short floor,
        SeatArea area,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        WaitlistStatus status,
        OffsetDateTime notifiedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {}
