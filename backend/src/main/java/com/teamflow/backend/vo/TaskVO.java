package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record TaskVO(
        Long id,
        Long projectId,
        String title,
        String description,
        Long creatorId,
        String creatorDisplayName,
        Long assigneeId,
        String assigneeDisplayName,
        String status,
        String priority,
        LocalDateTime dueAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
