package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.reservation.*;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.User;
import com.libseat.service.ExcelExportService;
import com.libseat.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ExcelExportService excelExportService;

    @GetMapping
    public Result<PageResult<ReservationResponse>> listMyReservations(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(reservationService.listMyReservations(
                currentUser.getId(), status, dateFrom, dateTo, page, pageSize));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportMyReservations(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        var rows = reservationService.listMyReservationsForExport(
                currentUser.getId(), status, dateFrom, dateTo);
        byte[] data = excelExportService.exportReservations(rows);
        return buildExcelResponse(data, "reservations-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/{reservationId}")
    public Result<ReservationResponse> getReservation(@AuthenticationPrincipal User currentUser,
                                                      @PathVariable UUID reservationId) {
        return Result.success(reservationService.getReservation(currentUser.getId(), reservationId));
    }

    @PostMapping
    public Result<ReservationResponse> createReservation(@AuthenticationPrincipal User currentUser,
                                                         @Valid @RequestBody CreateReservationRequest req) {
        return Result.success(reservationService.createReservation(currentUser.getId(), req));
    }

    @DeleteMapping("/{reservationId}")
    public Result<Void> cancelReservation(@AuthenticationPrincipal User currentUser,
                                          @PathVariable UUID reservationId,
                                          @Valid @RequestBody(required = false) CancelReservationRequest req) {
        reservationService.cancelReservation(currentUser.getId(), reservationId, req);
        return Result.success();
    }

    @PostMapping("/{reservationId}/checkin")
    public Result<Void> checkIn(@AuthenticationPrincipal User currentUser,
                                @PathVariable UUID reservationId,
                                @Valid @RequestBody CheckInRequest req) {
        reservationService.checkIn(currentUser.getId(), reservationId, req);
        return Result.success();
    }

    @PostMapping("/{reservationId}/renew")
    public Result<ReservationResponse> renew(@AuthenticationPrincipal User currentUser,
                                             @PathVariable UUID reservationId,
                                             @Valid @RequestBody RenewRequest req) {
        return Result.success(reservationService.renew(currentUser.getId(), reservationId, req));
    }

    static ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
