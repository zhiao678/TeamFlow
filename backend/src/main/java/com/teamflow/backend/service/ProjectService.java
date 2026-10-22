package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateProjectRequest;
import com.teamflow.backend.vo.ProjectDetailVO;

import java.util.List;

public interface ProjectService {

    List<ProjectDetailVO> listProjects();

    ProjectDetailVO getProjectById(Long id);

    ProjectDetailVO createProject(CreateProjectRequest request);
}
