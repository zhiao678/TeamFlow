package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Project;
import com.teamflow.backend.vo.ProjectDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ProjectMapper extends BaseMapper<Project> {

    @Select("""
            SELECT p.id,
                   p.recruitment_id,
                   r.title AS recruitment_title,
                   p.name,
                   p.description,
                   r.owner_id,
                   u.display_name AS owner_display_name,
                   p.status,
                   (SELECT COUNT(*) FROM team_member member
                    WHERE member.recruitment_id = p.recruitment_id AND member.status = 'ACTIVE') AS member_count,
                   pm.project_role AS current_user_role,
                   p.created_at,
                   p.updated_at
            FROM `project` p
            JOIN recruitment r ON r.id = p.recruitment_id AND r.status = 'TEAMED'
            JOIN `user` u ON u.id = r.owner_id
            JOIN team_member tm ON tm.recruitment_id = p.recruitment_id
                AND tm.user_id = #{userId} AND tm.status = 'ACTIVE'
            JOIN project_member pm ON pm.project_id = p.id
                AND pm.user_id = tm.user_id AND pm.status = 'ACTIVE'
            ORDER BY p.created_at DESC, p.id DESC
            """)
    List<ProjectDetailVO> selectForUser(@Param("userId") Long userId);

    @Select("""
            SELECT p.id,
                   p.recruitment_id,
                   r.title AS recruitment_title,
                   p.name,
                   p.description,
                   r.owner_id,
                   u.display_name AS owner_display_name,
                   p.status,
                   (SELECT COUNT(*) FROM team_member member
                    WHERE member.recruitment_id = p.recruitment_id AND member.status = 'ACTIVE') AS member_count,
                   p.created_at,
                   p.updated_at
            FROM `project` p
            JOIN recruitment r ON r.id = p.recruitment_id
            JOIN `user` u ON u.id = r.owner_id
            WHERE p.id = #{id}
            """)
    ProjectDetailVO selectProjectDetailById(@Param("id") Long id);

    @Select("SELECT * FROM project WHERE recruitment_id = #{recruitmentId}")
    Project selectByRecruitmentId(@Param("recruitmentId") Long recruitmentId);

    @Update("""
            UPDATE project SET owner_id = #{ownerId}, updated_at = CURRENT_TIMESTAMP
            WHERE recruitment_id = #{recruitmentId}
            """)
    int updateOwnerByRecruitment(@Param("recruitmentId") Long recruitmentId,
                                 @Param("ownerId") Long ownerId);
}
