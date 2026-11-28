package com.teamflow.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record TaskQueryRequest(
        @Pattern(regexp = "TODO|IN_PROGRESS|DONE") String status,
        @Positive Long assigneeId,
        @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer pageSize
) {
    public TaskQueryRequest {
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
    }
}
