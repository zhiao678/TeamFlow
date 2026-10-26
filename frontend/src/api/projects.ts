import { isAxiosError } from 'axios'
import { request } from './request'
import type { Project } from '@/types/project'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
export class ProjectRequestError extends Error {
  constructor(
    message: string,
    public notFound = false,
  ) {
    super(message)
  }
}
async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await request.get<ApiResponse<T>>(path, { signal })
  if (response.data.code !== 0) {
    throw new ProjectRequestError(
      response.data.message || '请求失败，请稍后重试',
      response.data.code === 40400,
    )
  }
  return response.data.data
}
export const getProjects = (signal?: AbortSignal) => get<Project[]>('/projects', signal)
export const getProject = (id: number, signal?: AbortSignal) =>
  get<Project>(`/projects/${id}`, signal)
export function projectError(error: unknown) {
  if (error instanceof ProjectRequestError) return error
  if (isAxiosError(error)) {
    return new ProjectRequestError(
      error.response?.status === 404
        ? '项目不存在或已被删除'
        : '无法加载项目，请检查网络及后端服务后重试',
      error.response?.status === 404,
    )
  }
  return new ProjectRequestError('无法加载项目，请稍后重试')
}
