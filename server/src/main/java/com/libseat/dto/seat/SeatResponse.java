package com.libseat.dto.seat;

import com.libseat.entity.SeatArea;
import com.libseat.entity.SeatStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatResponse(
        UUID id,
        UUID libraryId,
        String libraryName,
        String seatNo,
        short floor,
        SeatArea area,
        boolean hasComputer,
        boolean hasPower,
        boolean hasWindow,
        SeatStatus status,
        BigDecimal posX,
        BigDecimal posY
) {}
