package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.UpdateTaskStatusRequest;
import com.teamflow.backend.vo.TaskVO;

import java.util.List;

public interface TaskService {

    List<TaskVO> listTasks(Long projectId);

    TaskVO createTask(Long projectId, CreateTaskRequest request);

    TaskVO updateTaskStatus(Long taskId, UpdateTaskStatusRequest request);
}
