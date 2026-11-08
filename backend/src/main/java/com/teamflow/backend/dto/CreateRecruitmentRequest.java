package com.teamflow.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateRecruitmentRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 100) String courseName,
        @Size(max = 100) String teacherName,
        @NotBlank @Size(max = 200) String purpose,
        @Size(max = 20000) String description,
        @Size(max = 20000) String requirements,
        @NotNull @Min(2) Integer maxMembers,
        LocalDateTime deadlineAt,
        @Size(max = 20) List<@NotNull @Positive Long> tagIds,
        boolean publish
) {
}
