package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateTaskRequest;
import com.teamflow.backend.dto.TaskQueryRequest;
import com.teamflow.backend.dto.UpdateTaskRequest;
import com.teamflow.backend.vo.TaskPageVO;
import com.teamflow.backend.vo.TaskVO;

public interface TaskService {
    TaskPageVO list(Long projectId, TaskQueryRequest query);
    TaskVO create(Long projectId, CreateTaskRequest request);
    TaskVO update(Long id, UpdateTaskRequest request);
    TaskVO updateStatus(Long id, String status);
    void delete(Long id);
}
