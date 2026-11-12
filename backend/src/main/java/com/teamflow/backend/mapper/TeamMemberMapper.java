package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.vo.TeamMemberVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    @Select("SELECT * FROM team_member WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId}")
    TeamMember selectByRecruitmentAndUser(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM team_member WHERE recruitment_id = #{recruitmentId} AND status = 'ACTIVE'")
    long countActive(@Param("recruitmentId") Long recruitmentId);

    @Select("""
            SELECT tm.user_id AS id, u.display_name, u.avatar_url, tm.role, tm.joined_at
            FROM team_member tm
            JOIN `user` u ON u.id = tm.user_id
            WHERE tm.recruitment_id = #{recruitmentId} AND tm.status = 'ACTIVE'
            ORDER BY CASE WHEN tm.role = 'LEADER' THEN 0 ELSE 1 END, tm.joined_at, tm.user_id
            """)
    List<TeamMemberVO> selectActiveViews(@Param("recruitmentId") Long recruitmentId);

    @Update("""
            UPDATE team_member
            SET role = 'MEMBER', status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP, left_at = NULL
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId} AND status IN ('LEFT', 'REMOVED')
            """)
    int reactivateMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Update("""
            UPDATE team_member SET status = 'REMOVED', left_at = CURRENT_TIMESTAMP
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int removeMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Update("""
            UPDATE conversation_member SET status = 'REMOVED', left_at = CURRENT_TIMESTAMP
            WHERE user_id = #{userId} AND status = 'ACTIVE'
              AND conversation_id IN (
                  SELECT id FROM conversation WHERE recruitment_id = #{recruitmentId} AND type = 'TEAM'
              )
            """)
    int removeConversationMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);
}
