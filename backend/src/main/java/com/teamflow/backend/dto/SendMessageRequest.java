package com.teamflow.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 64) @JsonAlias("clientId") String clientMessageId,
        @NotBlank @Size(max = 2000) String content,
        @JsonAlias("recruitmentId") Long sourceRecruitmentId
) {
}
