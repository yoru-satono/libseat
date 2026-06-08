package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.PageResult;
import com.libseat.dto.notification.NotificationResponse;
import com.libseat.entity.NotificationType;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.JwtService;
import com.libseat.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class NotificationControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;

    @MockitoBean NotificationService notificationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    @Test
    void list_authenticated_returnsPage() throws Exception {
        NotificationResponse resp = new NotificationResponse(
                UUID.randomUUID(), NotificationType.RESERVATION_SUCCESS,
                "预约成功", "内容", null, false, null, OffsetDateTime.now());
        when(notificationService.listNotifications(eq(TEST_USER_ID), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(resp), 1, 1, 20, 1));

        mockMvc.perform(get("/v1/notifications").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("预约成功"));
    }

    @Test
    void list_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void list_unreadOnly_passesFilterToService() throws Exception {
        when(notificationService.listNotifications(eq(TEST_USER_ID), eq(true), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(List.of(), 0, 1, 20, 0));

        mockMvc.perform(get("/v1/notifications?unreadOnly=true").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(notificationService).listNotifications(eq(TEST_USER_ID), eq(true), anyInt(), anyInt());
    }

    @Test
    void markRead_authenticated_returnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/notifications/{id}/read", id).with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(notificationService).markRead(TEST_USER_ID, id);
    }

    @Test
    void markAllRead_authenticated_returnsSuccess() throws Exception {
        mockMvc.perform(patch("/v1/notifications/mark-all-read").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(notificationService).markAllRead(TEST_USER_ID);
    }

    @Test
    void delete_authenticated_returnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/v1/notifications/{id}", id).with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(notificationService).deleteNotification(TEST_USER_ID, id);
    }

    @Test
    void delete_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(delete("/v1/notifications/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }
}
