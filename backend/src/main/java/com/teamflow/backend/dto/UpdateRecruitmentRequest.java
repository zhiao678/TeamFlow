package com.teamflow.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateRecruitmentRequest(
        Long categoryId,
        @Size(max = 150) String title,
        @Size(max = 100) String courseName,
        @Size(max = 100) String teacherName,
        @Size(max = 200) String purpose,
        String description,
        String requirements,
        @Min(2) Integer maxMembers,
        LocalDateTime deadlineAt,
        List<Long> tagIds
) {
}
