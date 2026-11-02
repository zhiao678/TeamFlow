package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team_member")
public class TeamMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recruitmentId;
    private Long userId;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}
