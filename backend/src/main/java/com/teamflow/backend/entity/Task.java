package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;
    private Long creatorId;
    private Long assigneeId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime dueAt;
    private LocalDateTime completedAt;
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
