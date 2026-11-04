import { request } from './request'
import { isAxiosError } from 'axios'
import type { Recruitment } from '@/types/recruitment'

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
export async function getRecruitments(signal?: AbortSignal) {
  const result = await read<Recruitment[]>('/recruitments', signal)
  if (!Array.isArray(result)) throw new Error('招募列表格式已变化，请稍后重试。')
  return result.filter((item) => item.status !== 'DRAFT')
}
export const getRecruitment = (id: number, signal?: AbortSignal) =>
  read<Recruitment>(`/recruitments/${id}`, signal)
export function browseError(error: unknown) {
  const status =
    error instanceof BusinessError
      ? Math.floor(error.code / 100)
      : isAxiosError(error)
        ? error.response?.status
        : undefined
  if (status === 403) return '无权查看此招募内容。'
  if (status === 404) return '未找到招募记录。'
  return '加载失败，请检查网络或稍后重试。'
}
