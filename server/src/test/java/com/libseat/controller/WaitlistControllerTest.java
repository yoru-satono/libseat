package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.PageResult;
import com.libseat.dto.waitlist.WaitlistResponse;
import com.libseat.entity.SeatArea;
import com.libseat.entity.WaitlistStatus;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.JwtService;
import com.libseat.service.WaitlistService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WaitlistController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class WaitlistControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean WaitlistService waitlistService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final UUID WAITLIST_ID = UUID.randomUUID();

    @Test
    void join_authenticated_returnsSuccess() throws Exception {
        WaitlistResponse resp = sampleResponse(WaitlistStatus.WAITING);
        when(waitlistService.joinWaitlist(eq(TEST_USER_ID), any())).thenReturn(resp);

        String body = """
                {"seatId":"%s","date":"%s","startTime":"09:00","endTime":"11:00"}
                """.formatted(SEAT_ID, LocalDate.now());

        mockMvc.perform(post("/v1/waitlists")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    @Test
    void join_missingField_returnsParamError() throws Exception {
        mockMvc.perform(post("/v1/waitlists")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void join_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(post("/v1/waitlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void list_authenticated_returnsPage() throws Exception {
        when(waitlistService.listMyWaitlist(eq(TEST_USER_ID), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(sampleResponse(WaitlistStatus.WAITING)), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/waitlists").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void list_withStatusFilter_passesStatusToService() throws Exception {
        when(waitlistService.listMyWaitlist(eq(TEST_USER_ID), eq(WaitlistStatus.WAITING), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 0, 1, 20, 0));

        mockMvc.perform(get("/v1/waitlists?status=WAITING").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(waitlistService).listMyWaitlist(eq(TEST_USER_ID), eq(WaitlistStatus.WAITING), anyInt(), anyInt());
    }

    @Test
    void cancel_authenticated_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/v1/waitlists/{id}", WAITLIST_ID).with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(waitlistService).cancelWaitlist(TEST_USER_ID, WAITLIST_ID);
    }

    @Test
    void cancel_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(delete("/v1/waitlists/{id}", WAITLIST_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    private WaitlistResponse sampleResponse(WaitlistStatus status) {
        return new WaitlistResponse(
                WAITLIST_ID, SEAT_ID, "3F-001", "总馆", (short) 3, SeatArea.QUIET,
                LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(11, 0),
                status, null, OffsetDateTime.now().plusHours(2), OffsetDateTime.now());
    }
}
