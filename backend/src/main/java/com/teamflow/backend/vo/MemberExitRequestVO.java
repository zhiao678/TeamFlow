package com.teamflow.backend.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record MemberExitRequestVO(
        Long id,
        Long recruitmentId,
        String recruitmentTitle,
        Long userId,
        String userDisplayName,
        String status,
        String reason,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    @JsonProperty("handoverHint")
    public String handoverHint() {
        return "退出前请与负责人沟通当前分工和待办事项";
    }
}
