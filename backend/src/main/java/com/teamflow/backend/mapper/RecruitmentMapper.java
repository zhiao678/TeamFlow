package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.dto.RecruitmentQueryRequest;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.vo.RecruitmentVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RecruitmentMapper extends BaseMapper<Recruitment> {

    String DETAIL_COLUMNS = """
            SELECT r.id, r.owner_id, u.display_name AS owner_display_name,
                   r.category_id, c.name AS category_name, r.title, r.course_name,
                   r.teacher_name, r.purpose, r.description, r.requirements,
                   r.max_members,
                   (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS current_members,
                   r.max_members - (SELECT COUNT(*) FROM team_member tm
                    WHERE tm.recruitment_id = r.id AND tm.status = 'ACTIVE') AS remaining_slots,
                   r.deadline_at, r.status, r.close_reason, r.created_at, r.updated_at
            """;

    String DETAIL_FROM = """
            FROM recruitment r
            JOIN `user` u ON u.id = r.owner_id
            JOIN category c ON c.id = r.category_id
            """;

    String QUERY_FILTERS = """
            WHERE r.status = #{query.status}
            <if test="query.status == 'DRAFT'">
                AND r.owner_id = #{ownerId}
            </if>
            <if test="query.keyword != null and query.keyword != ''">
                AND (LOCATE(#{query.keyword}, r.title) &gt; 0
                     OR LOCATE(#{query.keyword}, r.purpose) &gt; 0
                     OR LOCATE(#{query.keyword}, r.course_name) &gt; 0
                     OR LOCATE(#{query.keyword}, r.teacher_name) &gt; 0)
            </if>
            <if test="query.categoryId != null">
                AND r.category_id = #{query.categoryId}
            </if>
            <if test="query.courseName != null and query.courseName != ''">
                AND LOCATE(#{query.courseName}, r.course_name) &gt; 0
            </if>
            <if test="query.teacherName != null and query.teacherName != ''">
                AND LOCATE(#{query.teacherName}, r.teacher_name) &gt; 0
            </if>
            """;

    @Select("<script>" + DETAIL_COLUMNS + DETAIL_FROM + QUERY_FILTERS + """
            <choose>
                <when test="query.sort == 'DEADLINE'">
                    ORDER BY CASE WHEN r.deadline_at IS NULL THEN 1 ELSE 0 END,
                             r.deadline_at ASC, r.id DESC
                </when>
                <otherwise>
                    ORDER BY r.created_at DESC, r.id DESC
                </otherwise>
            </choose>
            LIMIT #{query.pageSize} OFFSET #{offset}
            </script>
            """)
    List<RecruitmentVO> selectPage(
            @Param("query") RecruitmentQueryRequest query,
            @Param("ownerId") Long ownerId,
            @Param("offset") long offset
    );

    @Select("<script>SELECT COUNT(*) " + DETAIL_FROM + QUERY_FILTERS + "</script>")
    long countPage(@Param("query") RecruitmentQueryRequest query, @Param("ownerId") Long ownerId);

    @Select(DETAIL_COLUMNS + DETAIL_FROM + """
            WHERE r.owner_id = #{ownerId}
            ORDER BY r.created_at DESC, r.id DESC
            """)
    List<RecruitmentVO> selectMine(@Param("ownerId") Long ownerId);

    @Select(DETAIL_COLUMNS + DETAIL_FROM + "WHERE r.id = #{id}")
    RecruitmentVO selectDetail(@Param("id") Long id);

    @Select("SELECT * FROM recruitment WHERE id = #{id} FOR UPDATE")
    Recruitment selectForUpdate(@Param("id") Long id);

    @Select("""
            SELECT t.name
            FROM recruitment_tag rt
            JOIN tag t ON t.id = rt.tag_id
            WHERE rt.recruitment_id = #{recruitmentId}
            ORDER BY t.name
            """)
    List<String> selectTagNames(@Param("recruitmentId") Long recruitmentId);
}
