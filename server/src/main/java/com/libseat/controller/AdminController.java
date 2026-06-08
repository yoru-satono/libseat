package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.admin.*;
import com.libseat.dto.library.CreateLibraryRequest;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.dto.library.UpdateLibraryRequest;
import com.libseat.service.ExcelExportService;
import com.libseat.service.LibraryService;
import com.libseat.dto.user.ChangeRequestResponse;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.User;
import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;
import com.libseat.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final LibraryService libraryService;
    private final ExcelExportService excelExportService;

    // ── 用户管理 ───────────────────────────────────────────────────────────

    @GetMapping("/users")
    public Result<PageResult<AdminUserResponse>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminService.listUsers(role, status, page, pageSize));
    }

    @GetMapping("/users/{userId}")
    public Result<AdminUserResponse> getUser(@PathVariable UUID userId) {
        return Result.success(adminService.getUser(userId));
    }

    @PatchMapping("/users/{userId}")
    public Result<AdminUserResponse> updateUser(@AuthenticationPrincipal User currentUser,
                                                @PathVariable UUID userId,
                                                @Valid @RequestBody UpdateUserRequest req) {
        return Result.success(adminService.updateUser(currentUser.getId(), userId, req));
    }

    // ── 预约管理 ───────────────────────────────────────────────────────────

    @GetMapping("/reservations")
    public Result<PageResult<AdminReservationResponse>> listAllReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminService.listAllReservations(status, dateFrom, dateTo, page, pageSize));
    }

    @GetMapping("/reservations/export")
    public ResponseEntity<byte[]> exportReservations(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        var rows = adminService.listAllReservationsForExport(userId, status, dateFrom, dateTo);
        byte[] data = excelExportService.exportAdminReservations(rows);
        return ReservationController.buildExcelResponse(
                data, "reservations-admin-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/reservations/{reservationId}")
    public Result<AdminReservationResponse> getReservation(@PathVariable UUID reservationId) {
        return Result.success(adminService.getAdminReservation(reservationId));
    }

    @DeleteMapping("/reservations/{reservationId}")
    public Result<Void> cancelReservation(@AuthenticationPrincipal User currentUser,
                                          @PathVariable UUID reservationId) {
        adminService.adminCancelReservation(currentUser.getId(), reservationId);
        return Result.success();
    }

    // ── 信息修改申请 ───────────────────────────────────────────────────────

    @GetMapping("/change-requests")
    public Result<PageResult<AdminChangeRequestResponse>> listPendingChangeRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminService.listPendingChangeRequests(page, pageSize));
    }

    @PatchMapping("/change-requests/{requestId}")
    public Result<ChangeRequestResponse> handleChangeRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID requestId,
            @Valid @RequestBody HandleChangeRequestRequest req) {
        return Result.success(adminService.handleChangeRequest(requestId, currentUser.getId(), req));
    }

    // ── 系统规则 ───────────────────────────────────────────────────────────

    @GetMapping("/system-rules")
    public Result<SystemRulesResponse> getSystemRules() {
        return Result.success(adminService.getSystemRules());
    }

    @PutMapping("/system-rules")
    public Result<SystemRulesResponse> updateSystemRules(@AuthenticationPrincipal User currentUser,
                                                         @Valid @RequestBody UpdateSystemRulesRequest req) {
        return Result.success(adminService.updateSystemRules(currentUser.getId(), req));
    }

    // ── 图书馆管理 ─────────────────────────────────────────────────────────

    @GetMapping("/libraries")
    public Result<java.util.List<LibraryResponse>> listLibraries() {
        return Result.success(libraryService.listLibraries());
    }

    @GetMapping("/libraries/{id}")
    public Result<LibraryResponse> getLibrary(@PathVariable UUID id) {
        return Result.success(libraryService.getLibrary(id));
    }

    @PostMapping("/libraries")
    public Result<LibraryResponse> createLibrary(@Valid @RequestBody CreateLibraryRequest req) {
        return Result.success(libraryService.createLibrary(req));
    }

    @PutMapping("/libraries/{id}")
    public Result<LibraryResponse> updateLibrary(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateLibraryRequest req) {
        return Result.success(libraryService.updateLibrary(id, req));
    }

    @DeleteMapping("/libraries/{id}")
    public Result<Void> deleteLibrary(@PathVariable UUID id) {
        libraryService.deleteLibrary(id);
        return Result.success();
    }

    // ── 审计日志 ───────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminService.listAuditLogs(
                adminId, targetType, targetId, dateFrom, dateTo, page, pageSize));
    }
}
