package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.user.*;
import com.libseat.entity.User;
import com.libseat.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<UserProfileResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        return Result.success(userService.getProfile(currentUser.getId()));
    }

    @PatchMapping
    public Result<UserProfileResponse> updateProfile(@AuthenticationPrincipal User currentUser,
                                                     @Valid @RequestBody UpdateProfileRequest req) {
        return Result.success(userService.updateProfile(currentUser.getId(), req));
    }

    @PostMapping("/change-requests")
    public Result<ChangeRequestResponse> createChangeRequest(@AuthenticationPrincipal User currentUser,
                                                             @Valid @RequestBody ChangeFieldRequest req) {
        return Result.success(userService.createChangeRequest(currentUser.getId(), req));
    }

    @GetMapping("/change-requests")
    public Result<PageResult<ChangeRequestResponse>> listChangeRequests(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(userService.listMyChangeRequests(currentUser.getId(), page, pageSize));
    }
}
