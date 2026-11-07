import axios from 'axios'
import { request } from './request'
import type { Recruitment, RecruitmentInput, RecruitmentTagOption } from '@/types/recruitment'

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

/** 接口尚未提供：404（无此路径）或 405（方法不允许）。 */
export function isEndpointMissing(error: unknown) {
  const status = statusOf(error)
  return status === 404 || status === 405
}

/**
 * `GET /recruitments/mine` 未实现时，请求会被 `GET /recruitments/{id}` 接住，
 * `mine` 无法解析成数字 id，于是返回 400 而不是 404，这里一并视为“接口未提供”。
 */
export function isMyRecruitmentsUnavailable(error: unknown) {
  const status = statusOf(error)
  return status === 400 || status === 404 || status === 405
}

export function managementMessage(error: unknown) {
  const status = statusOf(error)
  if (status === 400) return '提交内容不符合要求，请检查必填项和人数设置。'
  if (status === 401) return '登录状态已失效，请重新登录后再操作。'
  if (status === 403) return '无权操作该招募：只有负责人可以编辑、发布或关闭。'
  if (status === 404) return '未找到该招募记录，可能已被删除或不是你的招募。'
  if (status === 405) return '该操作所需的接口尚未提供，请先联系后端补齐。'
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
  options: { includeTagIds: boolean },
) {
  const body: Omit<RecruitmentInput, 'tagIds'> & { tagIds?: number[] } = { ...input }
  if (!options.includeTagIds) delete body.tagIds
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

/** 后端尚未实现：`DELETE /api/recruitments/{id}`。保留封装，接口到位即可直接使用。 */
export function deleteRecruitmentDraft(id: number) {
  return unwrap<null>(request.delete<Result<null>>(`/recruitments/${id}`))
}

/** 后端尚未实现：`GET /api/recruitments/mine`。 */
export async function listMyRecruitments() {
  const data = await unwrap<Recruitment[] | { records: Recruitment[] }>(
    request.get<Result<Recruitment[] | { records: Recruitment[] }>>('/recruitments/mine'),
  )
  if (Array.isArray(data)) return data
  return data?.records ?? []
}

/**
 * 临时回退：`/recruitments/mine` 未实现时，从大厅列表里按负责人过滤。
 * 调用方必须把这次回退标注出来，不能当作正式的“我的招募”接口。
 */
export async function listRecruitmentsForOwnerFallback() {
  const data = await unwrap<Recruitment[]>(request.get<Result<Recruitment[]>>('/recruitments'))
  return Array.isArray(data) ? data : []
}

/** 后端尚未实现：`GET /api/categories`、`GET /api/tags`。 */
export const listCategories = () =>
  unwrap<RecruitmentTagOption[]>(request.get<Result<RecruitmentTagOption[]>>('/categories'))
export const listTags = () =>
  unwrap<RecruitmentTagOption[]>(request.get<Result<RecruitmentTagOption[]>>('/tags'))

/**
 * 与 `database/recruitment-init.sql` 种子数据一致的临时选项。
 * 仅在选项接口缺失时使用，界面上必须明确标注。
 */
export const FALLBACK_CATEGORIES: RecruitmentTagOption[] = [
  { id: 1, name: '课堂组队' },
  { id: 2, name: '学科竞赛' },
  { id: 3, name: '校园活动' },
  { id: 4, name: '其他' },
]

export const FALLBACK_TAGS: RecruitmentTagOption[] = [
  { id: 1, name: 'Java' },
  { id: 2, name: 'Vue' },
  { id: 3, name: '数据分析' },
  { id: 4, name: '文案整理' },
]
