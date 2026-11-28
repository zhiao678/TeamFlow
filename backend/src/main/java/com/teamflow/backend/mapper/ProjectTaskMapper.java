package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.dto.TaskQueryRequest;
import com.teamflow.backend.entity.ProjectTask;
import com.teamflow.backend.vo.TaskStatisticsVO;
import com.teamflow.backend.vo.TaskVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectTaskMapper extends BaseMapper<ProjectTask> {
    String VIEW_SQL = """
            SELECT t.id, t.project_id, t.title, t.description, t.creator_id,
                   creator.display_name AS creator_display_name, t.assignee_id,
                   assignee.display_name AS assignee_display_name, t.status, t.priority,
                   t.due_at, t.completed_at, t.created_at, t.updated_at
            FROM project_task t
            LEFT JOIN `user` creator ON creator.id = t.creator_id
            LEFT JOIN `user` assignee ON assignee.id = t.assignee_id
            """;

    String QUERY_FILTERS = """
            WHERE t.project_id = #{projectId}
            <if test="query.status != null">
                AND t.status = #{query.status}
            </if>
            <if test="query.assigneeId != null">
                AND t.assignee_id = #{query.assigneeId}
            </if>
            <if test="query.priority != null">
                AND t.priority = #{query.priority}
            </if>
            <if test="query.dueFrom != null">
                AND t.due_at &gt;= #{query.dueFrom}
            </if>
            <if test="query.dueTo != null">
                AND t.due_at &lt;= #{query.dueTo}
            </if>
            """;

    @Select("<script>" + VIEW_SQL + QUERY_FILTERS + """
            ORDER BY t.created_at DESC, t.id DESC
            LIMIT #{query.pageSize} OFFSET #{offset}
            </script>
            """)
    List<TaskVO> selectPage(@Param("projectId") Long projectId,
                            @Param("query") TaskQueryRequest query, @Param("offset") long offset);

    @Select("<script>" + """
            SELECT COALESCE(SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END), 0) AS todo,
                   COALESCE(SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0) AS in_progress,
                   COALESCE(SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END), 0) AS done
            FROM project_task t
            """ + QUERY_FILTERS + "</script>")
    TaskStatisticsVO selectStatistics(@Param("projectId") Long projectId, @Param("query") TaskQueryRequest query);

    @Select(VIEW_SQL + " WHERE t.id = #{id}")
    TaskVO selectView(@Param("id") Long id);

    @Select("SELECT project_id FROM project_task WHERE id = #{id}")
    Long selectProjectId(@Param("id") Long id);

    @Select("SELECT * FROM project_task WHERE id = #{id} FOR UPDATE")
    ProjectTask selectForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE project_task
            SET title = #{title}, description = #{description}, assignee_id = #{assigneeId},
                priority = #{priority}, due_at = #{dueAt}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateContent(ProjectTask task);

    @Update("""
            UPDATE project_task SET status = #{status}, completed_at = #{completedAt}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("completedAt") LocalDateTime completedAt, @Param("updatedAt") LocalDateTime updatedAt);
}
