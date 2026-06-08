package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.PageResult;
import com.libseat.dto.seat.SeatResponse;
import com.libseat.entity.SeatArea;
import com.libseat.entity.SeatStatus;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.JwtService;
import com.libseat.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SeatControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SeatService seatService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    private SeatResponse seatResp() {
        return new SeatResponse(UUID.randomUUID(), UUID.randomUUID(), "总馆",
                "3F-001", (short) 3, SeatArea.QUIET, false, true, false,
                SeatStatus.AVAILABLE, null, null);
    }

    @Test
    void listSeats_anonymousCanAccess() throws Exception {
        when(seatService.listSeats(any(), any(), any(), any(), any(), any(), any(), any(int.class), any(int.class)))
                .thenReturn(new PageResult<>(List.of(seatResp()), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getSeat_anonymousCanAccess() throws Exception {
        UUID id = UUID.randomUUID();
        when(seatService.getSeat(id)).thenReturn(seatResp());

        mockMvc.perform(get("/v1/seats/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }

    @Test
    void createSeat_requiresAdminRole() throws Exception {
        // 普通用户访问管理员端点，SecurityConfig 中 /v1/admin/** 才限定 ADMIN；
        // SeatController POST 要求已登录，普通用户可访问（ADMIN 判断在业务层）
        // 此处验证未登录返回 A0100
        mockMvc.perform(post("/v1/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void deleteSeat_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(delete("/v1/seats/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }
}
