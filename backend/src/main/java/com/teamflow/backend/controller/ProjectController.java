package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateProjectRequest;
import com.teamflow.backend.service.ProjectService;
import com.teamflow.backend.vo.ProjectDetailVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public Result<List<ProjectDetailVO>> listProjects() {
        return Result.success(projectService.listProjects());
    }

    @GetMapping("/{id}")
    public Result<ProjectDetailVO> getProject(
            @PathVariable @Positive(message = "must be greater than 0") Long id
    ) {
        return Result.success(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<Result<ProjectDetailVO>> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectDetailVO project = projectService.createProject(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.success(project));
    }
}
