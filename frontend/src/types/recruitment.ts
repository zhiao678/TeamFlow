export type RecruitmentStatus = 'DRAFT' | 'RECRUITING' | 'TEAMED' | 'CLOSED'
export interface PageResult<T> {
  records: T[]
  page: number
  pageSize: number
  total: number
}
export interface RecruitmentQuery {
  keyword?: string
  categoryId?: number
  courseName?: string
  teacherName?: string
  status: Exclude<RecruitmentStatus, 'DRAFT'>
  page: number
  pageSize: number
  sort: 'LATEST' | 'DEADLINE'
}
export interface Recruitment {
  id: number
  ownerId: number
  ownerDisplayName: string
  categoryId: number
  categoryName: string
  title: string
  courseName: string | null
  teacherName: string | null
  purpose: string
  description: string | null
  requirements: string | null
  maxMembers: number
  currentMembers: number
  remainingSlots: number
  deadlineAt: string | null
  status: RecruitmentStatus
  closeReason: string | null
  tags: string[]
  createdAt: string
  updatedAt: string
}
export interface RecruitmentTagOption {
  id: number
  name: string
}

/** 新建与编辑共用的一份表单状态；空字符串表示“未填写”。 */
export interface RecruitmentFormState {
  categoryId: number | null
  title: string
  courseName: string
  teacherName: string
  purpose: string
  description: string
  requirements: string
  maxMembers: number | null
  /** `yyyy-MM-ddTHH:mm:ss`，北京时间；空字符串表示不设置截止时间。 */
  deadlineAt: string
  tagIds: number[]
}

/** 提交给服务端的招募字段；负责人由服务端从 Token 解析。 */
export interface RecruitmentInput {
  categoryId: number
  title: string
  courseName: string
  teacherName: string
  purpose: string
  description: string
  requirements: string
  maxMembers: number
  deadlineAt: string | null
  tagIds: number[]
}

export const statusLabels = {
  DRAFT: '草稿',
  RECRUITING: '招募中',
  TEAMED: '已成队',
  CLOSED: '已关闭',
} as const
// The API sends local ISO dates without an offset, in Asia/Shanghai.
export const timestamp = (value: string) => Date.parse(value + '+08:00')
export const displayDate = (value: string | null) =>
  value ? value.replace('T', ' ').slice(0, 16) : '未设置'
export function displayStatus(item: Recruitment, now = Date.now()) {
  if (item.status !== 'RECRUITING') return statusLabels[item.status]
  if (item.deadlineAt && timestamp(item.deadlineAt) <= now) return '已截止'
  return item.remainingSlots <= 0 ? '已满员' : '招募中'
}
