package com.libseat.controller;

import com.libseat.config.SecurityConfig;
import com.libseat.dto.auth.LoginRequest;
import com.libseat.dto.auth.RegisterRequest;
import com.libseat.dto.auth.TokenPairResponse;
import com.libseat.exception.BusinessException;
import com.libseat.common.ErrorCode;
import com.libseat.repository.UserRepository;
import com.libseat.security.JwtAuthenticationFilter;
import com.libseat.service.AuthService;
import com.libseat.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest extends ControllerTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository userRepository;

    @Test
    void register_validRequest_returnsSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUserNo("S001"); req.setRealName("张三");
        req.setPassword("password123"); req.setEmail("a@b.com");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }

    @Test
    void register_missingRequiredField_returnsParamError() throws Exception {
        // 缺少 realName 等必填字段
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void login_success_returnsTokenPair() throws Exception {
        when(authService.login(any())).thenReturn(new TokenPairResponse("acc", "ref", 7200L));

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("pass");
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.accessToken").value("acc"));
    }

    @Test
    void login_wrongCredentials_returnsA0600() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BusinessException(ErrorCode.WRONG_CREDENTIALS));

        LoginRequest req = new LoginRequest(); req.setUserNo("S001"); req.setPassword("wrong");
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0600"));
    }

    @Test
    void register_weakPassword_returnsParamError() throws Exception {
        // 纯字母密码，不含数字，不满足 @Pattern
        RegisterRequest req = new RegisterRequest();
        req.setUserNo("S002"); req.setRealName("李四");
        req.setPassword("weakpassword"); req.setEmail("b@b.com");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A0400"));
    }

    @Test
    void confirmEmailChange_success() throws Exception {
        mockMvc.perform(post("/v1/auth/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"sometoken\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(authService).confirmEmailChange("sometoken");
    }

    @Test
    void logout_returnsSuccess() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }
}
