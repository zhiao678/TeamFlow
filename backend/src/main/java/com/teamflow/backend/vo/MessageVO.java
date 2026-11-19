package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record MessageVO(
        Long id,
        Long conversationId,
        Long senderId,
        String senderDisplayName,
        String clientMessageId,
        Long sourceRecruitmentId,
        String content,
        String status,
        LocalDateTime createdAt
) {
}
