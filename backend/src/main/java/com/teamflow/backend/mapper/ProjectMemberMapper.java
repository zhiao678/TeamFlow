package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.ProjectMember;
import com.teamflow.backend.vo.ProjectMemberVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("""
            SELECT pm.* FROM project_member pm
            JOIN project p ON p.id = pm.project_id
            JOIN team_member tm ON tm.recruitment_id = p.recruitment_id
                AND tm.user_id = pm.user_id AND tm.status = 'ACTIVE'
            WHERE pm.project_id = #{projectId} AND pm.user_id = #{userId} AND pm.status = 'ACTIVE'
            """)
    ProjectMember selectActiveMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Select("""
            SELECT pm.user_id AS id, u.display_name, u.avatar_url,
                   pm.project_role, tm.role AS team_role, pm.joined_at
            FROM project_member pm
            JOIN `user` u ON u.id = pm.user_id
            JOIN project p ON p.id = pm.project_id
            JOIN team_member tm ON tm.recruitment_id = p.recruitment_id
                AND tm.user_id = pm.user_id AND tm.status = 'ACTIVE'
            WHERE pm.project_id = #{projectId} AND pm.status = 'ACTIVE'
            ORDER BY CASE WHEN tm.role = 'LEADER' THEN 0 ELSE 1 END,
                     pm.joined_at, pm.user_id
            """)
    List<ProjectMemberVO> selectActiveViews(@Param("projectId") Long projectId);

    @Update("""
            UPDATE project_member SET status = #{status}, left_at = CURRENT_TIMESTAMP
            WHERE project_id = (SELECT id FROM project WHERE recruitment_id = #{recruitmentId})
              AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int revokeForRecruitmentMember(@Param("recruitmentId") Long recruitmentId,
                                   @Param("userId") Long userId,
                                   @Param("status") String status);

    @Update("""
            UPDATE project_member SET project_role = #{projectRole}
            WHERE project_id = (SELECT id FROM project WHERE recruitment_id = #{recruitmentId})
              AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int updateRoleForRecruitmentMember(@Param("recruitmentId") Long recruitmentId,
                                       @Param("userId") Long userId,
                                       @Param("projectRole") String projectRole);
}
