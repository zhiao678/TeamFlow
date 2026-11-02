package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_profile")
public class UserProfile {

    @TableId
    private Long userId;
    private String department;
    private String grade;
    private String bio;
    private Integer contactAllowed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
