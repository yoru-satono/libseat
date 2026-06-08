package com.libseat.dto.admin;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SystemRulesResponse(
        Long id,
        UUID libraryId,
        String libraryName,
        LocalTime openTimeStart,
        LocalTime openTimeEnd,
        short advanceDaysMax,
        short singleMinMinutes,
        short singleMaxHours,
        short dailyMaxHours,
        short checkinEarlyMinutes,
        short checkinLateMinutes,
        short noShowThreshold,
        short suspendDays,
        OffsetDateTime updatedAt
) {}
