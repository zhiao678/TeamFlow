import { request } from './request'
import { ApplicationError } from './applications'
import { isAxiosError } from 'axios'
import type { Recruitment, RecruitmentQuery, PageResult } from '@/types/recruitment'

class BusinessError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}
async function read<T>(path: string, signal?: AbortSignal) {
  const { data } = await request.get<{ code: number; message: string; data: T }>(path, { signal })
  if (data.code !== 0) throw new BusinessError(data.code, data.message)
  return data.data
}
export async function getRecruitments(params: RecruitmentQuery, signal?: AbortSignal) {
  const { data } = await request.get<{
    code: number
    message: string
    data: PageResult<Recruitment>
  }>('/recruitments', { params, signal })
  if (data.code !== 0) throw new BusinessError(data.code, data.message)
  return data.data
}
export const getRecruitment = (id: number, signal?: AbortSignal) =>
  read<Recruitment>(`/recruitments/${id}`, signal)
export function browseError(error: unknown) {
  const status =
    error instanceof BusinessError || error instanceof ApplicationError
      ? Math.floor(error.code / 100)
      : isAxiosError(error)
        ? error.response?.status
        : undefined
  if (status === 400) return '查询参数不符合要求，请检查筛选条件。'
  if (status === 401) return '登录状态已失效，请重新登录。'
  if (status === 409) return '招募状态已变化，请刷新后重试。'
  if (status === 500) return '服务端暂时无法处理请求，请稍后重试。'
  if (status === 403) return '无权查看此招募内容。'
  if (status === 404) return '未找到招募记录。'
  return '加载失败，请检查网络或稍后重试。'
}
