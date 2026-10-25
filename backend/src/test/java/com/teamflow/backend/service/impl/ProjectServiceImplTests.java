package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.dto.CreateProjectRequest;
import com.teamflow.backend.entity.Project;
import com.teamflow.backend.entity.ProjectMember;
import com.teamflow.backend.entity.User;
import com.teamflow.backend.mapper.ProjectMapper;
import com.teamflow.backend.mapper.ProjectMemberMapper;
import com.teamflow.backend.mapper.UserMapper;
import com.teamflow.backend.vo.ProjectDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTests {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void shouldCreateProjectAndAddOwnerAsAdmin() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName(" TeamFlow ");
        request.setDescription("Team collaboration system");
        request.setOwnerId(1L);

        User owner = new User();
        owner.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(owner);
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(10L);
            return 1;
        });
        when(projectMemberMapper.insert(any(ProjectMember.class))).thenReturn(1);

        ProjectDetailVO detail = new ProjectDetailVO();
        detail.setId(10L);
        detail.setName("TeamFlow");
        when(projectMapper.selectProjectDetailById(10L)).thenReturn(detail);

        ProjectDetailVO result = projectService.createProject(request);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertThat(projectCaptor.getValue().getName()).isEqualTo("TeamFlow");
        assertThat(projectCaptor.getValue().getOwnerId()).isEqualTo(1L);
        assertThat(projectCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberMapper).insert(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getProjectId()).isEqualTo(10L);
        assertThat(memberCaptor.getValue().getUserId()).isEqualTo(1L);
        assertThat(memberCaptor.getValue().getProjectRole()).isEqualTo("ADMIN");
        assertThat(result).isSameAs(detail);
    }

    @Test
    void shouldRejectUnknownOwner() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("TeamFlow");
        request.setOwnerId(999L);
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("owner user not found");

        verify(projectMapper, never()).insert(any(Project.class));
        verify(projectMemberMapper, never()).insert(any(ProjectMember.class));
    }

    @Test
    void shouldRejectUnknownProject() {
        when(projectMapper.selectProjectDetailById(999L)).thenReturn(null);

        assertThatThrownBy(() -> projectService.getProjectById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("project not found");
    }
}
