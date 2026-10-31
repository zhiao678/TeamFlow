package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("recruitment_tag")
public class RecruitmentTag {
    private Long recruitmentId;
    private Long tagId;
}
