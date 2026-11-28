package com.teamflow.backend.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class UpdateTaskRequest {
    @Size(max = 150)
    private String title;
    @Size(max = 2000)
    private String description;
    @Positive
    private Long assigneeId;
    @Pattern(regexp = "LOW|MEDIUM|HIGH")
    private String priority;
    private LocalDateTime dueAt;

    // PATCH must distinguish an omitted nullable field from an explicit null.
    private boolean titlePresent;
    private boolean descriptionPresent;
    private boolean assigneeIdPresent;
    private boolean priorityPresent;
    private boolean dueAtPresent;

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getAssigneeId() { return assigneeId; }
    public String getPriority() { return priority; }
    public LocalDateTime getDueAt() { return dueAt; }

    @JsonSetter
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonSetter
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }

    @JsonSetter
    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
        this.assigneeIdPresent = true;
    }

    @JsonSetter
    public void setPriority(String priority) {
        this.priority = priority;
        this.priorityPresent = true;
    }

    @JsonSetter
    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
        this.dueAtPresent = true;
    }

    public boolean hasTitle() { return titlePresent; }
    public boolean hasDescription() { return descriptionPresent; }
    public boolean hasAssigneeId() { return assigneeIdPresent; }
    public boolean hasPriority() { return priorityPresent; }
    public boolean hasDueAt() { return dueAtPresent; }

    public boolean hasChanges() {
        return titlePresent || descriptionPresent || assigneeIdPresent || priorityPresent || dueAtPresent;
    }
}
