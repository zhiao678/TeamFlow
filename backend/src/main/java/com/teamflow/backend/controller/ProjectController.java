package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.service.ProjectService;
import com.teamflow.backend.vo.ProjectDetailVO;
import com.teamflow.backend.vo.ProjectMemberVO;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/projects")
@Tag(name = "项目协作")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "查询当前用户可访问的项目")
    public Result<List<ProjectDetailVO>> listProjects() {
        return Result.success(projectService.listProjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询项目详情")
    public Result<ProjectDetailVO> getProject(
            @PathVariable @Positive(message = "must be greater than 0") Long id
    ) {
        return Result.success(projectService.getProjectById(id));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "查询项目成员")
    public Result<List<ProjectMemberVO>> listMembers(
            @PathVariable @Positive(message = "must be greater than 0") Long id
    ) {
        return Result.success(projectService.listMembers(id));
    }
}
