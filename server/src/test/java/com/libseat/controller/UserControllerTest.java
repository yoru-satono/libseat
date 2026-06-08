package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.user.UserProfileResponse;
import com.libseat.entity.UserRole;
import com.libseat.entity.UserStatus;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.JwtService;
import com.libseat.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    @Test
    void getProfile_authenticated_returnsProfile() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                TEST_USER_ID, "S001", "测试用户", "test@test.com",
                null, null, UserRole.STUDENT, UserStatus.ACTIVE,
                (short) 0, null, OffsetDateTime.now());

        when(userService.getProfile(TEST_USER_ID)).thenReturn(profile);

        mockMvc.perform(get("/v1/users/me").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.userNo").value("S001"));
    }

    @Test
    void getProfile_unauthenticated_returnsA0100() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0100"));
    }

    @Test
    void listChangeRequests_authenticated_returnsPage() throws Exception {
        when(userService.listMyChangeRequests(TEST_USER_ID, 1, 20))
                .thenReturn(new com.libseat.dto.PageResult<>(java.util.List.of(), 0, 1, 20, 0));

        mockMvc.perform(get("/v1/users/me/change-requests").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void updateProfile_weakNewPassword_returnsParamError() throws Exception {
        // 纯字母密码，不含数字，不满足 @Pattern
        mockMvc.perform(patch("/v1/users/me")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"weakpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void getProfile_adminCanAccess() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                TEST_ADMIN_ID, "A001", "管理员", "admin@test.com",
                null, null, UserRole.ADMIN, UserStatus.ACTIVE,
                (short) 0, null, OffsetDateTime.now());
        when(userService.getProfile(TEST_ADMIN_ID)).thenReturn(profile);

        mockMvc.perform(get("/v1/users/me").with(authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }
}
