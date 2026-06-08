package com.libseat.dto.admin;

import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminReservationResponse(
        UUID id,
        UUID userId,
        String userNo,
        String realName,
        UUID seatId,
        String seatNo,
        String libraryName,
        Short floor,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        SeatArea area,
        ReservationStatus status,
        OffsetDateTime checkinAt,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt
) {}
