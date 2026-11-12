package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("leader_transfer")
public class LeaderTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long fromUserId;
    private Long toUserId;
    private String status;
    private Boolean exitAfterTransfer;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
