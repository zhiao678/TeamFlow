package com.teamflow.backend.vo;

public record ConversationReadVO(
        Long conversationId,
        Long lastReadMessageId,
        long unreadCount
) {
}
