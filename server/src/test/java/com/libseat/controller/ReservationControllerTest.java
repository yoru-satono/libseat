package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.PageResult;
import com.libseat.dto.reservation.CreateReservationRequest;
import com.libseat.dto.reservation.ReservationResponse;
import com.libseat.entity.ReservationStatus;
import com.libseat.entity.SeatArea;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.ExcelExportService;
import com.libseat.service.JwtService;
import com.libseat.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ReservationControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ReservationService reservationService;
    @MockitoBean ExcelExportService excelExportService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private ReservationResponse reservationResp() {
        return new ReservationResponse(
                UUID.randomUUID(), UUID.randomUUID(), "3F-001", "总馆",
                (short) 3, SeatArea.QUIET,
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0),
                ReservationStatus.ACTIVE, null, null, null, OffsetDateTime.now());
    }

    @Test
    void listMyReservations_authenticated_returnsPage() throws Exception {
        when(reservationService.listMyReservations(any(), any(), any(), any(), any(int.class), any(int.class)))
                .thenReturn(new PageResult<>(List.of(reservationResp()), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/reservations").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listMyReservations_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void createReservation_validRequest_returnsSuccess() throws Exception {
        when(reservationService.createReservation(any(), any())).thenReturn(reservationResp());

        CreateReservationRequest req = new CreateReservationRequest();
        req.setSeatId(UUID.randomUUID()); req.setDate(LocalDate.now());
        req.setStartTime(LocalTime.of(9, 0)); req.setEndTime(LocalTime.of(11, 0));

        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }

    @Test
    void createReservation_missingFields_returnsA0400() throws Exception {
        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void export_authenticated_returnsExcelFile() throws Exception {
        when(reservationService.listMyReservationsForExport(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(excelExportService.exportReservations(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/v1/reservations/export").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void export_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/reservations/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }
}
