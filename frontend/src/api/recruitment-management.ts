import axios from 'axios'
import { request } from './request'
import type { Recruitment, RecruitmentInput } from '@/types/recruitment'

interface Result<T> {
  code: number
  message: string
  data: T
}

/** 服务端业务码（40000/40300…）包装，便于页面区分 403、404、409。 */
class BusinessError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}

async function unwrap<T>(call: Promise<{ data: Result<T> }>) {
  const { data } = await call
  if (data.code !== 0) throw new BusinessError(data.code, data.message)
  return data.data
}

function statusOf(error: unknown): number | undefined {
  if (error instanceof BusinessError) return Math.floor(error.code / 100)
  if (axios.isAxiosError(error)) return error.response?.status
  return undefined
}

export function managementMessage(error: unknown) {
  const status = statusOf(error)
  if (status === 400) return '提交内容不符合要求，请检查必填项和人数设置。'
  if (status === 401) return '登录状态已失效，请重新登录后再操作。'
  if (status === 403) return '无权操作该招募：只有负责人可以编辑、发布或关闭。'
  if (status === 404) return '未找到该招募记录，可能已被删除或不是你的招募。'
  if (status === 409) return '当前状态不允许此操作，请刷新后重试。'
  if (error instanceof Error && error.message) return error.message
  return '操作失败，请检查网络或后端服务后重试。'
}

export function createRecruitment(input: RecruitmentInput, publish: boolean) {
  return unwrap<Recruitment>(
    request.post<Result<Recruitment>>('/recruitments', { ...input, publish }),
  )
}

/**
 * `PATCH` 以 `null` 表示“不修改”，因此空文本一律提交空字符串，
 * 只有 `tagIds` 需要按回填情况决定是否提交（见 editor 中的说明）。
 */
export function updateRecruitment(
  id: number,
  input: RecruitmentInput,
  options: { includeTagIds: boolean; status: Recruitment['status'] },
) {
  const body: Omit<RecruitmentInput, 'tagIds'> & { tagIds?: number[] } = { ...input }
  if (!options.includeTagIds) delete body.tagIds
  if (options.status === 'RECRUITING') {
    const { description, requirements, tagIds, deadlineAt, maxMembers } = body
    return unwrap<Recruitment>(
      request.patch<Result<Recruitment>>(`/recruitments/${id}`, {
        description,
        requirements,
        tagIds,
        deadlineAt,
        maxMembers,
      }),
    )
  }
  if (options.status !== 'DRAFT') throw new Error('当前状态不允许编辑')
  return unwrap<Recruitment>(request.patch<Result<Recruitment>>(`/recruitments/${id}`, body))
}

export function publishRecruitment(id: number) {
  return unwrap<Recruitment>(request.post<Result<Recruitment>>(`/recruitments/${id}/publish`))
}

export function closeRecruitment(id: number, reason: string) {
  return unwrap<Recruitment>(
    request.post<Result<Recruitment>>(`/recruitments/${id}/close`, reason ? { reason } : {}),
  )
}

export function deleteRecruitmentDraft(id: number) {
  return unwrap<null>(request.delete<Result<null>>(`/recruitments/${id}`))
}
export function listMyRecruitments() {
  return unwrap<Recruitment[]>(request.get<Result<Recruitment[]>>('/recruitments/mine'))
}
export { listCategories, listTags } from './dictionaries'
