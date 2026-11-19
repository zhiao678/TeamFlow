package com.teamflow.backend.dto;

import jakarta.validation.constraints.Positive;

public record MarkConversationReadRequest(
        @Positive Long lastReadMessageId,
        @Positive Long messageId
) {
    public Long resolvedMessageId() {
        return lastReadMessageId != null ? lastReadMessageId : messageId;
    }
}
