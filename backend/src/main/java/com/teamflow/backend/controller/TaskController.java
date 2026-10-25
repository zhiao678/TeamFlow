package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.UpdateTaskStatusRequest;
import com.teamflow.backend.service.TaskService;
import com.teamflow.backend.vo.TaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
@Tag(name = "任务管理")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    @Operation(summary = "查询项目任务列表", description = "仅返回未逻辑删除的项目任务")
    public Result<List<TaskVO>> listTasks(
            @PathVariable @Positive(message = "must be greater than 0") Long projectId
    ) {
        return Result.success(taskService.listTasks(projectId));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @Operation(summary = "创建项目任务", description = "创建者和非空负责人必须为项目成员；新任务状态为 TODO")
    public ResponseEntity<Result<TaskVO>> createTask(
            @PathVariable @Positive(message = "must be greater than 0") Long projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        TaskVO task = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(task));
    }

    @PatchMapping("/tasks/{taskId}/status")
    @Operation(summary = "修改任务状态", description = "支持 TODO、IN_PROGRESS、DONE，完成时间随状态自动维护")
    public Result<TaskVO> updateTaskStatus(
            @PathVariable @Positive(message = "must be greater than 0") Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return Result.success(taskService.updateTaskStatus(taskId, request));
    }
}
