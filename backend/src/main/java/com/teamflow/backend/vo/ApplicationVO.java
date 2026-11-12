package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record ApplicationVO(
        Long id,
        Long recruitmentId,
        String recruitmentTitle,
        Long applicantId,
        String applicantDisplayName,
        String introduction,
        String skillsDescription,
        String availableTime,
        String status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
}
