package com.teamflow.backend.vo;

import java.time.LocalDateTime;
import java.util.List;

public class RecruitmentVO {
    private Long id;
    private Long ownerId;
    private String ownerDisplayName;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String courseName;
    private String teacherName;
    private String purpose;
    private String description;
    private String requirements;
    private Integer maxMembers;
    private Integer currentMembers;
    private Integer remainingSlots;
    private LocalDateTime deadlineAt;
    private String status;
    private String closeReason;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerDisplayName() { return ownerDisplayName; }
    public void setOwnerDisplayName(String ownerDisplayName) { this.ownerDisplayName = ownerDisplayName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }
    public Integer getCurrentMembers() { return currentMembers; }
    public void setCurrentMembers(Integer currentMembers) { this.currentMembers = currentMembers; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
    public LocalDateTime getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
