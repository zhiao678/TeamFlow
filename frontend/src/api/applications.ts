import axios from 'axios'
import { request } from './request'
import type { Application } from '@/types/application'

interface Result<T> {
  code: number
  message: string
  data: T
}

/** 服务端业务码包装，便于区分 403、404、409。 */
export class ApplicationError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

async function unwrap<T>(call: Promise<{ data: Result<T> }>) {
  const { data } = await call
  if (data.code !== 0) throw new ApplicationError(data.code, data.message)
  return data.data
}

/** HTTP 状态；业务码按 `floor(code / 100)` 还原。 */
export function applicationStatus(error: unknown): number | undefined {
  if (error instanceof ApplicationError) return Math.floor(error.code / 100)
  if (axios.isAxiosError(error)) return error.response?.status
  return undefined
}

export function reviewMessage(error: unknown) {
  const status = applicationStatus(error)
  if (status === 400) return '提交内容不符合要求，请检查拒绝原因是否超过 255 个字符。'
  if (status === 401) return '登录状态已失效，请重新登录后再操作。'
  if (status === 403) return '只有招募负责人可以查看和处理申请。'
  if (status === 404) return '未找到该申请或招募，可能已被删除。'
  if (status === 409) return '名额或状态已发生变化，请刷新后重新判断。'
  if (status === 500) return '服务端处理失败，请稍后重试。'
  if (error instanceof Error && error.message) return error.message
  return '操作失败，请检查网络或后端服务后重试。'
}

export function listRecruitmentApplications(recruitmentId: number, signal?: AbortSignal) {
  return unwrap<Application[]>(
    request.get<Result<Application[]>>(`/recruitments/${recruitmentId}/applications`, { signal }),
  )
}

export function acceptApplication(id: number) {
  return unwrap<Application>(request.patch<Result<Application>>(`/applications/${id}/accept`))
}

export function rejectApplication(id: number, reason: string) {
  return unwrap<Application>(
    request.patch<Result<Application>>(`/applications/${id}/reject`, reason ? { reason } : {}),
  )
}
