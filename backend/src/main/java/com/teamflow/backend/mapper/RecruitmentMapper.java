package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.vo.RecruitmentVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RecruitmentMapper extends BaseMapper<Recruitment> {

    @Select("""
            SELECT r.id, r.owner_id, u.display_name AS owner_display_name,
                   r.category_id, c.name AS category_name, r.title, r.course_name,
                   r.teacher_name, r.purpose, r.description, r.requirements,
                   r.max_members,
                   (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS current_members,
                   r.max_members - (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS remaining_slots,
                   r.deadline_at, r.status, r.close_reason, r.created_at, r.updated_at
            FROM recruitment r
            JOIN `user` u ON u.id = r.owner_id
            JOIN category c ON c.id = r.category_id
            WHERE r.status = 'RECRUITING'
               OR (#{ownerId} IS NOT NULL AND r.owner_id = #{ownerId})
            ORDER BY r.created_at DESC, r.id DESC
            """)
    List<RecruitmentVO> selectVisibleList(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT r.id, r.owner_id, u.display_name AS owner_display_name,
                   r.category_id, c.name AS category_name, r.title, r.course_name,
                   r.teacher_name, r.purpose, r.description, r.requirements,
                   r.max_members,
                   (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS current_members,
                   r.max_members - (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS remaining_slots,
                   r.deadline_at, r.status, r.close_reason, r.created_at, r.updated_at
            FROM recruitment r
            JOIN `user` u ON u.id = r.owner_id
            JOIN category c ON c.id = r.category_id
            WHERE r.id = #{id}
            """)
    RecruitmentVO selectDetail(@Param("id") Long id);

    @Select("""
            SELECT t.name
            FROM recruitment_tag rt
            JOIN tag t ON t.id = rt.tag_id
            WHERE rt.recruitment_id = #{recruitmentId}
            ORDER BY t.name
            """)
    List<String> selectTagNames(@Param("recruitmentId") Long recruitmentId);
}
