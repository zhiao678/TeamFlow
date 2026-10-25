package com.teamflow.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.teamflow.backend.common.enums.TaskPriority;
import com.teamflow.backend.common.enums.TaskStatus;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.UpdateTaskStatusRequest;
import com.teamflow.backend.entity.ProjectMember;
import com.teamflow.backend.entity.Task;
import com.teamflow.backend.mapper.ProjectMapper;
import com.teamflow.backend.mapper.ProjectMemberMapper;
import com.teamflow.backend.mapper.TaskMapper;
import com.teamflow.backend.service.TaskService;
import com.teamflow.backend.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public TaskServiceImpl(
            TaskMapper taskMapper,
            ProjectMapper projectMapper,
            ProjectMemberMapper projectMemberMapper
    ) {
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
    }

    @Override
    public List<TaskVO> listTasks(Long projectId) {
        requireProject(projectId);
        return taskMapper.selectTaskList(projectId);
    }

    @Override
    @Transactional
    public TaskVO createTask(Long projectId, CreateTaskRequest request) {
        requireProject(projectId);
        requireMember(projectId, request.getCreatorId(), "creator must be a project member");
        if (request.getAssigneeId() != null) {
            requireMember(projectId, request.getAssigneeId(), "assignee must be a project member");
        }

        Task task = new Task();
        task.setProjectId(projectId);
        task.setCreatorId(request.getCreatorId());
        task.setAssigneeId(request.getAssigneeId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setStatus(TaskStatus.TODO.name());
        task.setPriority(request.getPriority() == null ? TaskPriority.MEDIUM.name() : request.getPriority());
        task.setDueAt(request.getDueAt());
        task.setDeleted(0);
        taskMapper.insert(task);
        return requireTaskDetail(task.getId());
    }

    @Override
    @Transactional
    public TaskVO updateTaskStatus(Long taskId, UpdateTaskStatusRequest request) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getProjectId() == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project task not found");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime completedAt = TaskStatus.DONE.name().equals(request.getStatus())
                ? (task.getCompletedAt() == null ? now : task.getCompletedAt())
                : null;

        // Explicitly set NULL when reopening a completed task; updateById skips null fields by default.
        taskMapper.update(null, Wrappers.<Task>lambdaUpdate()
                .eq(Task::getId, taskId)
                .set(Task::getStatus, request.getStatus())
                .set(Task::getCompletedAt, completedAt)
                .set(Task::getUpdatedAt, now));
        return requireTaskDetail(taskId);
    }

    private void requireProject(Long projectId) {
        if (projectMapper.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project not found");
        }
    }

    private void requireMember(Long projectId, Long userId, String message) {
        Long count = projectMemberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
        }
    }

    private TaskVO requireTaskDetail(Long taskId) {
        TaskVO task = taskMapper.selectTaskDetailById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project task not found");
        }
        return task;
    }
}
