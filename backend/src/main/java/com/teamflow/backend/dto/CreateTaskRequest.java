package com.teamflow.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CreateTaskRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 200, message = "must not exceed 200 characters")
    @Schema(description = "任务标题", example = "完成任务接口联调")
    private String title;

    @Schema(description = "任务描述，可不填")
    private String description;

    @NotNull(message = "must not be null")
    @Positive(message = "must be greater than 0")
    @Schema(description = "创建者用户 ID，必须是项目成员；当前演示通过请求传递", example = "1")
    private Long creatorId;

    @Positive(message = "must be greater than 0")
    @Schema(description = "负责人用户 ID，可不填；填写时必须是项目成员", example = "2")
    private Long assigneeId;

    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "must be LOW, MEDIUM or HIGH")
    @Schema(description = "优先级，省略时为 MEDIUM", allowableValues = {"LOW", "MEDIUM", "HIGH"})
    private String priority;

    @Schema(description = "截止时间，可不填", example = "2026-10-30T18:00:00")
    private LocalDateTime dueAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }
}
