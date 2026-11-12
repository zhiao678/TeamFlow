package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record NotificationVO(
        Long id,
        String eventType,
        String targetType,
        Long recruitmentId,
        Long applicationId,
        String title,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
}
