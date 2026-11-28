package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.TaskQueryRequest;
import com.teamflow.backend.dto.UpdateTaskRequest;
import com.teamflow.backend.dto.UpdateTaskStatusRequest;
import com.teamflow.backend.service.TaskService;
import com.teamflow.backend.vo.TaskPageVO;
import com.teamflow.backend.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "项目任务")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "分页查询项目任务及筛选统计")
    public Result<TaskPageVO> list(@PathVariable @Positive Long projectId,
                                    @Valid @ModelAttribute TaskQueryRequest query) {
        return Result.success(taskService.list(projectId, query));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "创建项目任务")
    public ResponseEntity<Result<TaskVO>> create(@PathVariable @Positive Long projectId,
                                                 @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(taskService.create(projectId, request)));
    }

    @PatchMapping("/tasks/{id}")
    @Operation(summary = "修改项目任务内容和指派")
    public Result<TaskVO> update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return Result.success(taskService.update(id, request));
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "修改任务状态")
    public Result<TaskVO> updateStatus(@PathVariable @Positive Long id,
                                      @Valid @RequestBody UpdateTaskStatusRequest request) {
        return Result.success(taskService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "删除项目任务")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        taskService.delete(id);
        return Result.success();
    }
}
