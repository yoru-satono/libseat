package com.libseat.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
public class UpdateSystemRulesRequest {

    @NotNull
    private LocalTime openTimeStart;

    @NotNull
    private LocalTime openTimeEnd;

    @NotNull
    @Min(1) @Max(30)
    private Short advanceDaysMax;

    @NotNull
    @Min(15)
    private Short singleMinMinutes;

    @NotNull
    @Min(1) @Max(12)
    private Short singleMaxHours;

    @NotNull
    @Min(1) @Max(24)
    private Short dailyMaxHours;

    @NotNull
    @Min(0)
    private Short checkinEarlyMinutes;

    @NotNull
    @Min(0)
    private Short checkinLateMinutes;

    @NotNull
    @Min(1)
    private Short noShowThreshold;

    @NotNull
    @Min(1)
    private Short suspendDays;
}
