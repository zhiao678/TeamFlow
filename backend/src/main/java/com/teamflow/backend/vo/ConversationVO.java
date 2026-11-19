package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record ConversationVO(
        Long id,
        String type,
        Long recruitmentId,
        String status,
        LocalDateTime createdAt,
        Long otherUserId,
        String otherUserDisplayName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
