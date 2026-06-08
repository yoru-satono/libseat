package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.PageResult;
import com.libseat.dto.admin.AdminReservationResponse;
import com.libseat.dto.admin.AdminUserResponse;
import com.libseat.dto.admin.AuditLogResponse;
import com.libseat.dto.library.LibraryResponse;
import com.libseat.service.LibraryService;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;
import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.AdminService;
import com.libseat.service.ExcelExportService;
import com.libseat.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AdminControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminService adminService;
    @MockitoBean LibraryService libraryService;
    @MockitoBean ExcelExportService excelExportService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private AdminUserResponse adminUserResp() {
        return new AdminUserResponse(
                UUID.randomUUID(), "S001", "张三", "a@b.com",
                null, null, UserRole.STUDENT, UserStatus.ACTIVE,
                (short) 0, (short) 0, null, null,
                null, OffsetDateTime.now());
    }

    @Test
    void listUsers_adminCanAccess() throws Exception {
        when(adminService.listUsers(any(), any(), any(int.class), any(int.class)))
                .thenReturn(new PageResult<>(List.of(adminUserResp()), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/admin/users").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listUsers_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void listUsers_regularUserForbidden_returnsA0301() throws Exception {
        mockMvc.perform(get("/v1/admin/users").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0301"));
    }

    // ── libraries ─────────────────────────────────────────────────────────

    @Test
    void listLibraries_adminCanAccess() throws Exception {
        when(libraryService.listLibraries()).thenReturn(List.of(
                new LibraryResponse(UUID.randomUUID(), "总馆", "校园路1号", null, OffsetDateTime.now())));

        mockMvc.perform(get("/v1/admin/libraries").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data[0].name").value("总馆"));
    }

    @Test
    void listLibraries_regularUserForbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/libraries").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0301"));
    }

    // ── audit-logs ────────────────────────────────────────────────────────

    @Test
    void listAuditLogs_adminCanAccess_returnsPage() throws Exception {
        AuditLogResponse entry = new AuditLogResponse(
                1L, TEST_ADMIN_ID, "管理员", "UPDATE_USER", "user",
                UUID.randomUUID().toString(), Map.of("status", "ACTIVE"), null, OffsetDateTime.now());
        when(adminService.listAuditLogs(any(), any(), any(), any(), any(), any(int.class), any(int.class)))
                .thenReturn(new PageResult<>(List.of(entry), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/admin/audit-logs").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].actionType").value("UPDATE_USER"));
    }

    @Test
    void listAuditLogs_regularUserForbidden_returnsA0301() throws Exception {
        mockMvc.perform(get("/v1/admin/audit-logs").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0301"));
    }

    @Test
    void listAuditLogs_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    // ── reservation ───────────────────────────────────────────────────────

    private AdminReservationResponse adminReservationResp() {
        return new AdminReservationResponse(
                UUID.randomUUID(), TEST_ADMIN_ID, "S001", "张三",
                UUID.randomUUID(), "A001", "总馆", (short) 2,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0),
                SeatArea.QUIET, ReservationStatus.ACTIVE,
                null, null, OffsetDateTime.now());
    }

    @Test
    void getReservation_adminCanAccess() throws Exception {
        UUID rid = UUID.randomUUID();
        when(adminService.getAdminReservation(rid)).thenReturn(adminReservationResp());

        mockMvc.perform(get("/v1/admin/reservations/{id}", rid).with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.seatNo").value("A001"));
    }

    @Test
    void getReservation_regularUserForbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/reservations/{id}", UUID.randomUUID())
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0301"));
    }

    @Test
    void cancelReservation_adminCanCancel() throws Exception {
        UUID rid = UUID.randomUUID();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/v1/admin/reservations/{id}", rid)
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        org.mockito.Mockito.verify(adminService).adminCancelReservation(TEST_ADMIN_ID, rid);
    }

    // ── reservation export ────────────────────────────────────────────────

    @Test
    void exportReservations_adminCanExport() throws Exception {
        when(adminService.listAllReservationsForExport(any(), any(), any(), any()))
                .thenReturn(java.util.List.of());
        when(excelExportService.exportAdminReservations(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/v1/admin/reservations/export").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    void exportReservations_regularUserForbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/reservations/export").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0301"));
    }

    @Test
    void getSystemRules_adminCanAccess() throws Exception {
        when(adminService.getSystemRules()).thenReturn(
                new com.libseat.dto.admin.SystemRulesResponse(
                        1L, null, null,
                        java.time.LocalTime.of(7, 0), java.time.LocalTime.of(22, 0),
                        (short) 7, (short) 30, (short) 4, (short) 8,
                        (short) 10, (short) 15, (short) 3, (short) 7,
                        OffsetDateTime.now()));

        mockMvc.perform(get("/v1/admin/system-rules").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.advanceDaysMax").value(7));
    }
}
