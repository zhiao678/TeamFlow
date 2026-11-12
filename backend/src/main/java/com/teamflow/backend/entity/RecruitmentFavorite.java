package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("recruitment_favorite")
public class RecruitmentFavorite {
    private Long userId;
    private Long recruitmentId;
    private LocalDateTime createdAt;
}
