package com.teamflow.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("conversation_member")
public class ConversationMember {
    private Long conversationId;
    private Long userId;
    private String status;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Long lastReadMessageId;
}
