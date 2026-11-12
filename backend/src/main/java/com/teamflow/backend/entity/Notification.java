package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientId;
    private Long actorId;
    private String eventType;
    private String eventKey;
    private String targetType;
    private Long recruitmentId;
    private Long applicationId;
    private Long leaderTransferId;
    private Long reportId;
    private String title;
    private String content;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
