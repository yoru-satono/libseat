package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.auth.*;
import com.libseat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return Result.success();
    }

    @PostMapping("/activate")
    public Result<Void> activate(@Valid @RequestBody ActivateRequest req) {
        authService.activate(req.getToken());
        return Result.success();
    }

    @PostMapping("/login")
    public Result<TokenPairResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.success(authService.login(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，登出由客户端丢弃 Token 完成
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<TokenPairResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return Result.success(authService.refresh(req));
    }

    @PostMapping("/password/reset-request")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return Result.success();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return Result.success();
    }

    @PostMapping("/email/confirm")
    public Result<Void> confirmEmailChange(@Valid @RequestBody ActivateRequest req) {
        authService.confirmEmailChange(req.getToken());
        return Result.success();
    }
}
