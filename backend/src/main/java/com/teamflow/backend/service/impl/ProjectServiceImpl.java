package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.enums.ProjectRole;
import com.teamflow.backend.common.enums.ProjectStatus;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateProjectRequest;
import com.teamflow.backend.entity.Project;
import com.teamflow.backend.entity.ProjectMember;
import com.teamflow.backend.mapper.ProjectMapper;
import com.teamflow.backend.mapper.ProjectMemberMapper;
import com.teamflow.backend.mapper.UserMapper;
import com.teamflow.backend.service.ProjectService;
import com.teamflow.backend.vo.ProjectDetailVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final UserMapper userMapper;

    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            ProjectMemberMapper projectMemberMapper,
            UserMapper userMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<ProjectDetailVO> listProjects() {
        return projectMapper.selectProjectList();
    }

    @Override
    public ProjectDetailVO getProjectById(Long id) {
        ProjectDetailVO project = projectMapper.selectProjectDetailById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project not found");
        }
        return project;
    }

    @Override
    @Transactional
    public ProjectDetailVO createProject(CreateProjectRequest request) {
        if (userMapper.selectById(request.getOwnerId()) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "owner user not found");
        }

        Project project = new Project();
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setOwnerId(request.getOwnerId());
        project.setStatus(ProjectStatus.ACTIVE.name());
        projectMapper.insert(project);

        ProjectMember ownerMembership = new ProjectMember();
        ownerMembership.setProjectId(project.getId());
        ownerMembership.setUserId(request.getOwnerId());
        ownerMembership.setProjectRole(ProjectRole.ADMIN.name());
        projectMemberMapper.insert(ownerMembership);

        ProjectDetailVO createdProject = projectMapper.selectProjectDetailById(project.getId());
        if (createdProject == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "failed to load the created project"
            );
        }
        return createdProject;
    }
}
