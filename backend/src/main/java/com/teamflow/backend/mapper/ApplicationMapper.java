package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Application;
import com.teamflow.backend.vo.ApplicationVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ApplicationMapper extends BaseMapper<Application> {

    String VIEW_SQL = """
            SELECT a.id, a.recruitment_id, r.title AS recruitment_title,
                   a.applicant_id, u.display_name AS applicant_display_name,
                   a.introduction, a.skills_description, a.available_time, a.status,
                   a.rejection_reason, a.created_at, a.processed_at
            FROM application a
            JOIN recruitment r ON r.id = a.recruitment_id
            JOIN `user` u ON u.id = a.applicant_id
            """;

    @Select(VIEW_SQL + "WHERE a.id = #{id}")
    ApplicationVO selectView(@Param("id") Long id);

    @Select(VIEW_SQL + "WHERE a.recruitment_id = #{recruitmentId} ORDER BY a.created_at DESC, a.id DESC")
    List<ApplicationVO> selectForRecruitment(@Param("recruitmentId") Long recruitmentId);

    @Select(VIEW_SQL + "WHERE a.applicant_id = #{applicantId} ORDER BY a.created_at DESC, a.id DESC")
    List<ApplicationVO> selectForApplicant(@Param("applicantId") Long applicantId);

    @Select("SELECT recruitment_id FROM application WHERE id = #{id}")
    Long selectRecruitmentId(@Param("id") Long id);

    @Select("SELECT * FROM application WHERE id = #{id} FOR UPDATE")
    Application selectForUpdate(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM application WHERE recruitment_id = #{recruitmentId} AND applicant_id = #{applicantId} AND status = 'PENDING'")
    long countPending(@Param("recruitmentId") Long recruitmentId, @Param("applicantId") Long applicantId);

    @Select("SELECT * FROM application WHERE recruitment_id = #{recruitmentId} AND status = 'PENDING' ORDER BY id FOR UPDATE")
    List<Application> selectPendingForUpdate(@Param("recruitmentId") Long recruitmentId);

    @Update("""
            UPDATE application
            SET status = #{status}, rejection_reason = #{reason}, processed_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int process(@Param("id") Long id, @Param("status") String status, @Param("reason") String reason);
}
