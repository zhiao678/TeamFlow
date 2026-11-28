package com.teamflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @Positive Long assigneeId,
        @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
        LocalDateTime dueAt
) {
}
