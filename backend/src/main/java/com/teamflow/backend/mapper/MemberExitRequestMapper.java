package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.MemberExitRequest;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.vo.MemberExitRequestVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberExitRequestMapper extends BaseMapper<MemberExitRequest> {
    String VIEW_QUERY = """
            SELECT e.id, e.recruitment_id, r.title AS recruitment_title,
                   e.user_id, u.display_name AS user_display_name, e.status,
                   e.reason, e.rejection_reason, e.created_at, e.processed_at
            FROM member_exit_request e
            JOIN recruitment r ON r.id = e.recruitment_id
            JOIN `user` u ON u.id = e.user_id
            """;

    @Select("SELECT recruitment_id FROM member_exit_request WHERE id = #{id}")
    Long selectRecruitmentId(@Param("id") Long id);

    @Select("SELECT * FROM member_exit_request WHERE id = #{id} FOR UPDATE")
    MemberExitRequest selectForUpdate(@Param("id") Long id);

    @Select("""
            SELECT * FROM member_exit_request
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId} AND status = 'PENDING'
            FOR UPDATE
            """)
    MemberExitRequest selectPendingForMember(@Param("recruitmentId") Long recruitmentId,
                                             @Param("userId") Long userId);

    @Select("SELECT * FROM team_member WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId}")
    TeamMember selectMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Select(VIEW_QUERY + " WHERE e.id = #{id}")
    MemberExitRequestVO selectView(@Param("id") Long id);

    @Select(VIEW_QUERY + " WHERE e.recruitment_id = #{recruitmentId} ORDER BY e.created_at DESC, e.id DESC")
    List<MemberExitRequestVO> selectForRecruitment(@Param("recruitmentId") Long recruitmentId);

    @Select(VIEW_QUERY + " WHERE e.user_id = #{userId} ORDER BY e.created_at DESC, e.id DESC")
    List<MemberExitRequestVO> selectForUser(@Param("userId") Long userId);

    @Update("""
            UPDATE member_exit_request
            SET status = #{status}, rejection_reason = #{rejectionReason}, processed_at = #{processedAt}
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int process(@Param("id") Long id, @Param("status") String status,
                @Param("rejectionReason") String rejectionReason,
                @Param("processedAt") LocalDateTime processedAt);

    @Update("""
            UPDATE member_exit_request SET status = 'EXPIRED', processed_at = CURRENT_TIMESTAMP
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId} AND status = 'PENDING'
            """)
    int expirePendingForMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Update("""
            UPDATE team_member SET status = 'LEFT', left_at = #{leftAt}
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId}
              AND status = 'ACTIVE' AND role = 'MEMBER'
            """)
    int leaveMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId,
                    @Param("leftAt") LocalDateTime leftAt);

    @Update("""
            UPDATE conversation_member SET status = 'LEFT', left_at = #{leftAt}
            WHERE user_id = #{userId} AND status = 'ACTIVE'
              AND conversation_id IN (
                  SELECT id FROM conversation WHERE recruitment_id = #{recruitmentId} AND type = 'TEAM'
              )
            """)
    int revokeConversationAccess(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId,
                                 @Param("leftAt") LocalDateTime leftAt);
}
