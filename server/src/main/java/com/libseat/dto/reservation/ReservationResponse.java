package com.libseat.dto.reservation;

import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID seatId,
        String seatNo,
        String libraryName,
        short floor,
        SeatArea area,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        OffsetDateTime checkinAt,
        OffsetDateTime cancelledAt,
        String cancelReason,
        OffsetDateTime createdAt
) {}
