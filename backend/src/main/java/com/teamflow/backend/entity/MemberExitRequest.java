package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("member_exit_request")
public class MemberExitRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long userId;
    private String status;
    private String reason;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
