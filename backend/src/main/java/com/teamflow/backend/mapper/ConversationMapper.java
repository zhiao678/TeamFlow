package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Conversation;
import com.teamflow.backend.vo.ConversationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ConversationMapper extends BaseMapper<Conversation> {

    String VIEW_COLUMNS = """
            SELECT c.id, c.type, c.recruitment_id, c.status, c.created_at,
                   CASE WHEN c.type = 'DIRECT' AND c.direct_user_low_id = #{userId}
                        THEN c.direct_user_high_id
                        WHEN c.type = 'DIRECT' THEN c.direct_user_low_id END AS other_user_id,
                   CASE WHEN c.type = 'DIRECT'
                        THEN (SELECT u.display_name FROM `user` u
                              WHERE u.id = CASE WHEN c.direct_user_low_id = #{userId}
                                                THEN c.direct_user_high_id ELSE c.direct_user_low_id END)
                   END AS other_user_display_name,
                   (SELECT m.content FROM message m
                    WHERE m.conversation_id = c.id AND m.status = 'VISIBLE'
                    ORDER BY m.id DESC LIMIT 1) AS last_message,
                   (SELECT m.created_at FROM message m
                    WHERE m.conversation_id = c.id AND m.status = 'VISIBLE'
                    ORDER BY m.id DESC LIMIT 1) AS last_message_at,
                   (SELECT COUNT(*) FROM message m
                    WHERE m.conversation_id = c.id AND m.status = 'VISIBLE'
                      AND m.id > COALESCE(cm.last_read_message_id, 0)) AS unread_count
            FROM conversation c
            JOIN conversation_member cm ON cm.conversation_id = c.id
            """;

    @Select("SELECT * FROM conversation WHERE id = #{id}")
    Conversation selectConversation(@Param("id") Long id);

    @Select("SELECT * FROM conversation WHERE id = #{id} FOR UPDATE")
    Conversation selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM conversation WHERE type = 'TEAM' AND recruitment_id = #{recruitmentId}")
    Conversation selectTeamByRecruitment(@Param("recruitmentId") Long recruitmentId);

    @Select("""
            SELECT * FROM conversation
            WHERE type = 'DIRECT'
              AND direct_user_low_id = #{lowUserId}
              AND direct_user_high_id = #{highUserId}
            """)
    Conversation selectDirect(@Param("lowUserId") Long lowUserId,
                              @Param("highUserId") Long highUserId);

    @Select(VIEW_COLUMNS + """
            WHERE cm.conversation_id = #{conversationId}
              AND cm.user_id = #{userId}
              AND cm.status = 'ACTIVE'
              AND (c.type <> 'TEAM' OR EXISTS (
                  SELECT 1 FROM team_member tm
                  WHERE tm.recruitment_id = c.recruitment_id
                    AND tm.user_id = cm.user_id AND tm.status = 'ACTIVE'))
            """)
    ConversationVO selectView(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Select(VIEW_COLUMNS + """
            WHERE cm.user_id = #{userId}
              AND cm.status = 'ACTIVE'
              AND (c.type <> 'TEAM' OR EXISTS (
                  SELECT 1 FROM team_member tm
                  WHERE tm.recruitment_id = c.recruitment_id
                    AND tm.user_id = cm.user_id AND tm.status = 'ACTIVE'))
            ORDER BY c.created_at DESC, c.id DESC
            """)
    List<ConversationVO> selectForUser(@Param("userId") Long userId);
}
