import { isAxiosError } from 'axios'
import { request } from './request'
import type { Task, TaskStatus } from '@/types/task'

export interface CreateTaskPayload {
  title: string
  description?: string
  creatorId: number
  assigneeId?: number
  priority?: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAt?: string
}

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export class TaskRequestError extends Error {
  constructor(
    message: string,
    public notFound = false,
  ) {
    super(message)
  }
}

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise
  if (response.data.code !== 0) {
    throw new TaskRequestError(
      response.data.message || '请求失败，请稍后重试',
      response.data.code === 40400,
    )
  }
  return response.data.data
}

export const getProjectTasks = (projectId: number, signal?: AbortSignal) =>
  unwrap<Task[]>(request.get(`/projects/${projectId}/tasks`, { signal }))

export const createProjectTask = (projectId: number, payload: CreateTaskPayload) =>
  unwrap<Task>(request.post(`/projects/${projectId}/tasks`, payload))

export const updateTaskStatus = (taskId: number, status: TaskStatus) =>
  unwrap<Task>(request.patch(`/tasks/${taskId}/status`, { status }))

export function taskError(error: unknown, fallback: string) {
  if (error instanceof TaskRequestError) return error
  if (isAxiosError(error)) {
    const message = error.response?.data?.message
    return new TaskRequestError(
      typeof message === 'string' && message ? message : fallback,
      error.response?.status === 404,
    )
  }
  return new TaskRequestError(fallback)
}
