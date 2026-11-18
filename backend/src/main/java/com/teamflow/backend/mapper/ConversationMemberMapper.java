package com.teamflow.backend.mapper;

import com.teamflow.backend.entity.ConversationMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ConversationMemberMapper {

    @Insert("""
            INSERT INTO conversation_member
                (conversation_id, user_id, status, joined_at, left_at, last_read_message_id)
            VALUES (#{conversationId}, #{userId}, #{status}, CURRENT_TIMESTAMP, NULL, NULL)
            """)
    int insert(ConversationMember member);

    @Select("SELECT * FROM conversation_member WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    ConversationMember select(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Select("SELECT * FROM conversation_member WHERE conversation_id = #{conversationId} AND user_id = #{userId} FOR UPDATE")
    ConversationMember selectForUpdate(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("""
            UPDATE conversation_member
            SET status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP, left_at = NULL
            WHERE conversation_id = #{conversationId} AND user_id = #{userId}
            """)
    int reactivate(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("""
            UPDATE conversation_member
            SET last_read_message_id = #{messageId}
            WHERE conversation_id = #{conversationId} AND user_id = #{userId}
              AND status = 'ACTIVE'
              AND (last_read_message_id IS NULL OR last_read_message_id < #{messageId})
            """)
    int advanceRead(@Param("conversationId") Long conversationId,
                    @Param("userId") Long userId,
                    @Param("messageId") Long messageId);
}
