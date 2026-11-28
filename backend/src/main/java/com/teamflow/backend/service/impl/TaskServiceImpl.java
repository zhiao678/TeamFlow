package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.enums.ProjectRole;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.TaskQueryRequest;
import com.teamflow.backend.dto.UpdateTaskRequest;
import com.teamflow.backend.entity.ProjectTask;
import com.teamflow.backend.mapper.ProjectMapper;
import com.teamflow.backend.mapper.ProjectMemberMapper;
import com.teamflow.backend.mapper.ProjectTaskMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.service.ProjectService;
import com.teamflow.backend.service.TaskService;
import com.teamflow.backend.vo.ProjectDetailVO;
import com.teamflow.backend.vo.TaskPageVO;
import com.teamflow.backend.vo.TaskStatisticsVO;
import com.teamflow.backend.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final ProjectTaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final ProjectService projectService;

    public TaskServiceImpl(ProjectTaskMapper taskMapper, ProjectMapper projectMapper,
                           ProjectMemberMapper projectMemberMapper, RecruitmentMapper recruitmentMapper,
                           ProjectService projectService) {
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.recruitmentMapper = recruitmentMapper;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskPageVO list(Long projectId, TaskQueryRequest query) {
        projectService.getProjectById(projectId);
        if (query.dueFrom() != null && query.dueTo() != null && query.dueFrom().isAfter(query.dueTo())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "dueFrom cannot be later than dueTo");
        }
        TaskStatisticsVO statistics = taskMapper.selectStatistics(projectId, query);
        long total = statistics.todo() + statistics.inProgress() + statistics.done();
        long offset = ((long) query.page() - 1) * query.pageSize();
        List<TaskVO> records = taskMapper.selectPage(projectId, query, offset);
        return new TaskPageVO(records, query.page(), query.pageSize(), total, statistics);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskVO create(Long projectId, CreateTaskRequest request) {
        Long userId = CurrentUserContext.require();
        ProjectDetailVO project = lockProject(projectId);
        requireAdmin(project);
        requireAssignee(projectId, request.assigneeId());
        validateDueAt(request.dueAt());

        ProjectTask task = new ProjectTask();
        task.setProjectId(projectId);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setCreatorId(userId);
        task.setAssigneeId(request.assigneeId());
        task.setStatus("TODO");
        task.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
        task.setDueAt(request.dueAt());
        taskMapper.insert(task);
        return taskMapper.selectView(task.getId());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskVO update(Long id, UpdateTaskRequest request) {
        ProjectDetailVO project = lockProjectForTask(id);
        requireAdmin(project);
        ProjectTask task = requireTaskForUpdate(id);
        if (!request.hasChanges()) {
            return taskMapper.selectView(id);
        }
        if (request.hasTitle()) {
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "title cannot be blank");
            }
            task.setTitle(request.getTitle().trim());
        }
        if (request.hasDescription()) task.setDescription(request.getDescription());
        if (request.hasAssigneeId()) {
            requireAssignee(project.getId(), request.getAssigneeId());
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.hasPriority()) {
            if (request.getPriority() == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "priority cannot be null");
            }
            task.setPriority(request.getPriority());
        }
        if (request.hasDueAt()) {
            validateDueAt(request.getDueAt());
            task.setDueAt(request.getDueAt());
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateContent(task);
        return taskMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TaskVO updateStatus(Long id, String status) {
        Long userId = CurrentUserContext.require();
        ProjectDetailVO project = lockProjectForTask(id);
        ProjectTask task = requireTaskForUpdate(id);
        if (!ProjectRole.ADMIN.name().equals(project.getCurrentUserRole())
                && (!ProjectRole.MEMBER.name().equals(project.getCurrentUserRole())
                || !userId.equals(task.getAssigneeId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only project admins or the assignee can update task status");
        }
        if (!status.equals(task.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            taskMapper.updateStatus(id, status, "DONE".equals(status) ? now : null, now);
        }
        return taskMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id) {
        ProjectDetailVO project = lockProjectForTask(id);
        requireAdmin(project);
        requireTaskForUpdate(id);
        taskMapper.deleteById(id);
    }

    private ProjectDetailVO lockProjectForTask(Long taskId) {
        CurrentUserContext.require();
        Long projectId = taskMapper.selectProjectId(taskId);
        if (projectId == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "task not found");
        return lockProject(projectId);
    }

    private ProjectDetailVO lockProject(Long projectId) {
        CurrentUserContext.require();
        Long recruitmentId = projectMapper.selectRecruitmentId(projectId);
        if (recruitmentId == null || recruitmentMapper.selectForUpdate(recruitmentId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project not found");
        }
        return projectService.getProjectById(projectId);
    }

    private ProjectTask requireTaskForUpdate(Long id) {
        ProjectTask task = taskMapper.selectForUpdate(id);
        if (task == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "task not found");
        return task;
    }

    private void requireAdmin(ProjectDetailVO project) {
        if (!ProjectRole.ADMIN.name().equals(project.getCurrentUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only project admins can manage tasks");
        }
    }

    private void requireAssignee(Long projectId, Long userId) {
        if (userId != null && projectMemberMapper.selectActiveMember(projectId, userId) == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "assignee must be an active project and team member");
        }
    }

    private void validateDueAt(LocalDateTime dueAt) {
        if (dueAt != null && (dueAt.getYear() < 1000 || dueAt.getYear() > 9999)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "dueAt must be within years 1000 through 9999");
        }
    }
}
