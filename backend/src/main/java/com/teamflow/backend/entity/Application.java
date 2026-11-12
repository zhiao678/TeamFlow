package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long applicantId;
    private String introduction;
    private String skillsDescription;
    private String availableTime;
    private String status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
