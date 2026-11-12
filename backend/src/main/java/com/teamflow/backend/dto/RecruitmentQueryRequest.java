package com.teamflow.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecruitmentQueryRequest(
        @Size(max = 200) String keyword,
        @Positive Long categoryId,
        @Size(max = 100) String courseName,
        @Size(max = 100) String teacherName,
        @Size(max = 20) List<@NotNull @Positive Long> tagIds,
        @Min(0) Integer minRemainingSlots,
        @Pattern(regexp = "DRAFT|RECRUITING|TEAMED|CLOSED") String status,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer pageSize,
        @Pattern(regexp = "LATEST|DEADLINE|POPULAR") String sort
) {
    public RecruitmentQueryRequest {
        keyword = keyword == null ? null : keyword.trim();
        courseName = courseName == null ? null : courseName.trim();
        teacherName = teacherName == null ? null : teacherName.trim();
        tagIds = tagIds == null ? List.of() : tagIds.stream().distinct().toList();
        minRemainingSlots = minRemainingSlots == null ? null : minRemainingSlots;
        status = status == null ? "RECRUITING" : status;
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        sort = sort == null ? "LATEST" : sort;
    }
}
