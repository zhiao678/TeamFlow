package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Message;
import com.teamflow.backend.vo.MessageVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {

    String VIEW_COLUMNS = """
            SELECT m.id, m.conversation_id, m.sender_id, u.display_name AS sender_display_name,
                   m.client_message_id, m.source_recruitment_id, m.content, m.status, m.created_at
            FROM message m
            JOIN `user` u ON u.id = m.sender_id
            """;

    @Select(VIEW_COLUMNS + " WHERE m.id = #{id} AND m.conversation_id = #{conversationId}")
    MessageVO selectView(@Param("conversationId") Long conversationId, @Param("id") Long id);

    @Select("""
            SELECT * FROM message
            WHERE conversation_id = #{conversationId}
              AND sender_id = #{senderId}
              AND client_message_id = #{clientMessageId}
            """)
    Message selectByClientId(@Param("conversationId") Long conversationId,
                             @Param("senderId") Long senderId,
                             @Param("clientMessageId") String clientMessageId);

    @Select("SELECT COUNT(*) FROM message WHERE conversation_id = #{conversationId} AND id = #{messageId}")
    long countInConversation(@Param("conversationId") Long conversationId,
                             @Param("messageId") Long messageId);

    @Select("SELECT COUNT(*) FROM message WHERE conversation_id = #{conversationId} AND status = 'VISIBLE'")
    long countVisible(@Param("conversationId") Long conversationId);

    @Select("""
            SELECT COUNT(*) FROM message
            WHERE conversation_id = #{conversationId} AND status = 'VISIBLE'
              AND id > #{afterMessageId}
            """)
    long countVisibleAfter(@Param("conversationId") Long conversationId,
                           @Param("afterMessageId") Long afterMessageId);

    @Select("""
            SELECT COUNT(*) FROM message m
            WHERE m.conversation_id = #{conversationId} AND m.status = 'VISIBLE'
              AND m.id > COALESCE(#{lastReadMessageId}, 0)
            """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("lastReadMessageId") Long lastReadMessageId);

    @Select("""
            <script>
            SELECT m.id, m.conversation_id, m.sender_id, u.display_name AS sender_display_name,
                   m.client_message_id, m.source_recruitment_id, m.content, m.status, m.created_at
            FROM message m
            JOIN `user` u ON u.id = m.sender_id
            WHERE m.conversation_id = #{conversationId} AND m.status = 'VISIBLE'
            <if test="afterMessageId != null">
                AND m.id &gt; #{afterMessageId}
            </if>
            ORDER BY m.id ASC
            LIMIT #{pageSize} OFFSET #{offset}
            </script>
            """)
    List<MessageVO> selectPage(@Param("conversationId") Long conversationId,
                               @Param("afterMessageId") Long afterMessageId,
                               @Param("pageSize") int pageSize,
                               @Param("offset") long offset);
}
