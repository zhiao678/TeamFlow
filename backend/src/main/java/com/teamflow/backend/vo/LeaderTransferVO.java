package com.teamflow.backend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record LeaderTransferVO(
        Long id,
        Long recruitmentId,
        String recruitmentTitle,
        Long fromUserId,
        String fromDisplayName,
        Long toUserId,
        String toDisplayName,
        String status,
        boolean exitAfterTransfer,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    @JsonProperty("handoverHint")
    public String handoverHint() {
        return "请与继任负责人沟通当前职责和工作内容";
    }
}
