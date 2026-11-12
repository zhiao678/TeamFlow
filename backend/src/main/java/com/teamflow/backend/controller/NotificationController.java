package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.vo.NotificationPageVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<NotificationPageVO> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return Result.success(notificationService.list(page, pageSize, unreadOnly));
    }

    @PatchMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable @Positive Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @PatchMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success();
    }
}
