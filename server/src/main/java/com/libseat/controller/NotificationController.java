package com.libseat.controller;

import com.libseat.common.Result;
import com.libseat.dto.PageResult;
import com.libseat.dto.notification.NotificationResponse;
import com.libseat.entity.User;
import com.libseat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<PageResult<NotificationResponse>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(notificationService.listNotifications(
                currentUser.getId(), isRead, page, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationService.countUnread(currentUser.getId());
        return Result.success(Map.of("count", count));
    }

    @PatchMapping("/{id}/read")
    public Result<Void> markRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        notificationService.markRead(currentUser.getId(), id);
        return Result.success();
    }

    @PatchMapping("/read-all")
    public Result<Void> markAllRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllRead(currentUser.getId());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        notificationService.deleteNotification(currentUser.getId(), id);
        return Result.success();
    }
}
