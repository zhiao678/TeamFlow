import axios from 'axios'
import { request } from './request'
import type { TeamMember } from '@/types/application'

interface Result<T> {
  code: number
  message: string
  data: T
}

class MemberError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

export function membersError(error: unknown) {
  const status =
    error instanceof MemberError
      ? Math.floor(error.code / 100)
      : axios.isAxiosError(error)
        ? error.response?.status
        : undefined
  if (status === 403) return '无权查看该招募的成员。'
  if (status === 404) return '未找到该招募，可能已被删除或尚未公开。'
  return '成员加载失败，请检查网络或稍后重试。'
}

/** 公开接口：游客也能查询 ACTIVE 正式成员；草稿返回 404。 */
export async function listRecruitmentMembers(recruitmentId: number, signal?: AbortSignal) {
  const { data } = await request.get<Result<TeamMember[]>>(
    `/recruitments/${recruitmentId}/members`,
    { signal },
  )
  if (data.code !== 0) throw new MemberError(data.code, data.message)
  return data.data
}
