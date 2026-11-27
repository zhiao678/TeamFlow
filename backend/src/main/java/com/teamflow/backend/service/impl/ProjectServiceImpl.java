package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.enums.ProjectRole;
import com.teamflow.backend.common.enums.ProjectStatus;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.entity.Project;
import com.teamflow.backend.entity.ProjectMember;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.ProjectMapper;
import com.teamflow.backend.mapper.ProjectMemberMapper;
import com.teamflow.backend.mapper.TeamMemberMapper;
import com.teamflow.backend.service.ProjectService;
import com.teamflow.backend.vo.ProjectDetailVO;
import com.teamflow.backend.vo.ProjectMemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TeamMemberMapper teamMemberMapper;

    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            ProjectMemberMapper projectMemberMapper,
            TeamMemberMapper teamMemberMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.teamMemberMapper = teamMemberMapper;
    }

    @Override
    public List<ProjectDetailVO> listProjects() {
        return projectMapper.selectForUser(CurrentUserContext.require());
    }

    @Override
    public ProjectDetailVO getProjectById(Long id) {
        Long userId = CurrentUserContext.require();
        ProjectDetailVO project = projectMapper.selectProjectDetailById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "project not found");
        }

        TeamMember teamMember = teamMemberMapper.selectByRecruitmentAndUser(
                project.getRecruitmentId(), userId);
        ProjectMember projectMember = projectMemberMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, id)
                        .eq(ProjectMember::getUserId, userId)
                        .eq(ProjectMember::getStatus, "ACTIVE"));
        if (teamMember == null || !"ACTIVE".equals(teamMember.getStatus()) || projectMember == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "project is not accessible");
        }
        project.setCurrentUserRole(projectMember.getProjectRole());
        return project;
    }

    @Override
    public List<ProjectMemberVO> listMembers(Long id) {
        getProjectById(id);
        return projectMemberMapper.selectActiveViews(id);
    }

    @Override
    @Transactional
    public Project createForFormedRecruitment(Recruitment recruitment, List<TeamMember> activeMembers) {
        if (recruitment == null || recruitment.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "recruitment is required");
        }
        if (!"TEAMED".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "project can only be created for a formed team");
        }
        if (activeMembers == null || activeMembers.isEmpty()) {
            throw new BusinessException(ErrorCode.CONFLICT, "formed team must have active members");
        }
        if (projectMapper.selectByRecruitmentId(recruitment.getId()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "project already exists for this recruitment");
        }

        Project project = new Project();
        project.setRecruitmentId(recruitment.getId());
        project.setName(recruitment.getTitle());
        project.setDescription(projectDescription(recruitment));
        project.setOwnerId(recruitment.getOwnerId());
        project.setStatus(ProjectStatus.ACTIVE.name());
        try {
            projectMapper.insert(project);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "project already exists for this recruitment");
        }

        for (TeamMember member : activeMembers) {
            if (!"ACTIVE".equals(member.getStatus())) {
                throw new BusinessException(ErrorCode.CONFLICT, "only active team members can join a project");
            }
            ProjectMember projectMember = new ProjectMember();
            projectMember.setProjectId(project.getId());
            projectMember.setUserId(member.getUserId());
            projectMember.setProjectRole("LEADER".equals(member.getRole())
                    ? ProjectRole.ADMIN.name() : ProjectRole.MEMBER.name());
            projectMember.setStatus("ACTIVE");
            projectMemberMapper.insert(projectMember);
        }
        return project;
    }

    @Override
    @Transactional
    public void revokeMember(Long recruitmentId, Long userId, String status) {
        if (!"LEFT".equals(status) && !"REMOVED".equals(status)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "invalid project member status");
        }
        projectMemberMapper.revokeForRecruitmentMember(recruitmentId, userId, status);
    }

    @Override
    @Transactional
    public void transferLeader(Long recruitmentId, Long formerLeaderId, Long newLeaderId) {
        projectMapper.updateOwnerByRecruitment(recruitmentId, newLeaderId);
        projectMemberMapper.updateRoleForRecruitmentMember(
                recruitmentId, formerLeaderId, ProjectRole.MEMBER.name());
        projectMemberMapper.updateRoleForRecruitmentMember(
                recruitmentId, newLeaderId, ProjectRole.ADMIN.name());
    }

    private String projectDescription(Recruitment recruitment) {
        return Stream.of(recruitment.getPurpose(), recruitment.getDescription(), recruitment.getRequirements())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }
}
