package com.teamflow.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePrivateConversationRequest(
        @NotNull @Positive @JsonAlias({"userId", "targetId"}) Long targetUserId
) {
}
