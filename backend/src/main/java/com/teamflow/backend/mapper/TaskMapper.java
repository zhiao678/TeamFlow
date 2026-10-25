package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Task;
import com.teamflow.backend.vo.TaskVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TaskMapper extends BaseMapper<Task> {

    String TASK_COLUMNS = """
            SELECT t.id, t.project_id, t.creator_id,
                   creator.display_name AS creator_display_name,
                   t.assignee_id, assignee.display_name AS assignee_display_name,
                   t.title, t.description, t.status, t.priority,
                   t.due_at, t.completed_at, t.created_at, t.updated_at
            FROM `task` t
            JOIN `user` creator ON creator.id = t.creator_id
            LEFT JOIN `user` assignee ON assignee.id = t.assignee_id
            """;

    @Select(TASK_COLUMNS + """
            WHERE t.project_id = #{projectId} AND t.deleted = 0
            ORDER BY t.created_at DESC, t.id DESC
            """)
    List<TaskVO> selectTaskList(@Param("projectId") Long projectId);

    @Select(TASK_COLUMNS + """
            WHERE t.id = #{id} AND t.deleted = 0
            """)
    TaskVO selectTaskDetailById(@Param("id") Long id);
}
