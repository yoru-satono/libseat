package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.seat.*;
import com.libseat.entity.SeatArea;
import com.libseat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public Result<PageResult<SeatResponse>> listSeats(
            @RequestParam(required = false) Short floor,
            @RequestParam(required = false) SeatArea area,
            @RequestParam(required = false) Boolean hasComputer,
            @RequestParam(required = false) Boolean hasPower,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(seatService.listSeats(
                floor, area, hasComputer, hasPower, date, startTime, endTime, page, pageSize));
    }

    @GetMapping("/{seatId}")
    public Result<SeatResponse> getSeat(@PathVariable UUID seatId) {
        return Result.success(seatService.getSeat(seatId));
    }

    @PostMapping
    public Result<SeatResponse> createSeat(@Valid @RequestBody CreateSeatRequest req) {
        return Result.success(seatService.createSeat(req));
    }

    @PutMapping("/{seatId}")
    public Result<SeatResponse> updateSeat(@PathVariable UUID seatId,
                                           @Valid @RequestBody UpdateSeatRequest req) {
        return Result.success(seatService.updateSeat(seatId, req));
    }

    @DeleteMapping("/{seatId}")
    public Result<Void> deleteSeat(@PathVariable UUID seatId) {
        seatService.deleteSeat(seatId);
        return Result.success();
    }
}
