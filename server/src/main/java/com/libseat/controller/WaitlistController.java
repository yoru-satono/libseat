package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.waitlist.JoinWaitlistRequest;
import com.libseat.dto.waitlist.WaitlistResponse;
import com.libseat.entity.User;
import com.libseat.entity.WaitlistStatus;
import com.libseat.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/waitlists")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping
    public Result<WaitlistResponse> join(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody JoinWaitlistRequest req) {
        return Result.success(waitlistService.joinWaitlist(currentUser.getId(), req));
    }

    @GetMapping
    public Result<PageResult<WaitlistResponse>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) WaitlistStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(waitlistService.listMyWaitlist(
                currentUser.getId(), status, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> cancel(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        waitlistService.cancelWaitlist(currentUser.getId(), id);
        return Result.success();
    }
}
