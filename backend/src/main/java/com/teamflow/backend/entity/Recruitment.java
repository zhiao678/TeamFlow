package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("recruitment")
public class Recruitment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private Long categoryId;
    private String title;
    private String courseName;
    private String teacherName;
    private String purpose;
    private String description;
    private String requirements;
    private Integer maxMembers;
    private LocalDateTime deadlineAt;
    private String status;
    private String closeReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
