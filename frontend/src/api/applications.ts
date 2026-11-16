import axios from 'axios'
import { request } from './request'
import type { Application, ApplicationInput } from '@/types/application'

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

export function listMyApplications(signal?: AbortSignal) {
  return unwrap<Application[]>(request.get<Result<Application[]>>('/applications/me', { signal }))
}

export function submitApplication(recruitmentId: number, input: ApplicationInput) {
  return unwrap<Application>(
    request.post<Result<Application>>(`/recruitments/${recruitmentId}/applications`, input),
  )
}

export function withdrawApplication(id: number) {
  return unwrap<Application>(request.patch<Result<Application>>(`/applications/${id}/withdraw`))
}

export function applicantMessage(error: unknown) {
  const status = applicationStatus(error)
  const detail =
    error instanceof ApplicationError
      ? error.message
      : axios.isAxiosError(error)
        ? error.response?.data?.message
        : undefined
  if (status === 409) {
    const conflicts: Record<string, string> = {
      'recruitment deadline has passed': '招募已截止。',
      'current team members cannot apply': '你已是正式成员，不能重复申请。',
      'a pending application already exists': '你已有待处理申请，请勿重复提交。',
      'recruitment is not recruiting': '招募已成队或已关闭。',
      'application has already been processed': '该申请已处理，不能再撤回。',
      'recruitment has no remaining slots': '招募已满员，没有剩余名额。',
    }
    return (conflicts[detail] || detail || '申请或招募状态已发生变化。') + ' 请核对刷新后的状态。'
  }
  if (status === 400) return '提交内容不符合要求，请检查字段长度和筛选参数。'
  if (status === 401) return '登录状态已失效，请重新登录。'
  if (status === 403) return '你无权执行此操作。'
  if (status === 404) return '申请或招募不存在，可能已被删除。'
  return '请求失败，请检查网络或稍后重试。'
}
