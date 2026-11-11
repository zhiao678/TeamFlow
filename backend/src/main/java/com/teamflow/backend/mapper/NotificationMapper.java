package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface NotificationMapper extends BaseMapper<Notification> {
    @Insert("""
            INSERT INTO notification
                (recipient_id, actor_id, event_type, event_key, target_type, recruitment_id,
                 application_id, leader_transfer_id, report_id, title, content)
            VALUES
                (#{recipientId}, #{actorId}, #{eventType}, #{eventKey}, #{targetType}, #{recruitmentId},
                 #{applicationId}, #{leaderTransferId}, #{reportId}, #{title}, #{content})
            ON DUPLICATE KEY UPDATE event_key = event_key
            """)
    int insertIfAbsent(Notification notification);

    @Select("""
            SELECT * FROM notification
            WHERE recipient_id = #{userId}
              AND (#{unreadOnly} = 0 OR read_at IS NULL)
            ORDER BY created_at DESC, id DESC
            LIMIT #{pageSize} OFFSET #{offset}
            """)
    List<Notification> selectPageForUser(@Param("userId") Long userId,
                                         @Param("unreadOnly") boolean unreadOnly,
                                         @Param("pageSize") int pageSize,
                                         @Param("offset") long offset);

    @Select("SELECT COUNT(*) FROM notification WHERE recipient_id = #{userId} AND (#{unreadOnly} = 0 OR read_at IS NULL)")
    long countForUser(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly);

    @Select("SELECT COUNT(*) FROM notification WHERE recipient_id = #{userId} AND read_at IS NULL")
    long countUnread(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM notification WHERE id = #{id} AND recipient_id = #{userId}")
    long countOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notification SET read_at = COALESCE(read_at, CURRENT_TIMESTAMP) WHERE id = #{id} AND recipient_id = #{userId}")
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE notification SET read_at = CURRENT_TIMESTAMP WHERE recipient_id = #{userId} AND read_at IS NULL")
    int markAllRead(@Param("userId") Long userId);
}
