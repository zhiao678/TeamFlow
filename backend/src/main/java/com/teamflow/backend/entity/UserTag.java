package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_tag")
public class UserTag {
    private Long userId;
    private Long tagId;
}
