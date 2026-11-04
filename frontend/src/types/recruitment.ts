export type RecruitmentStatus = 'DRAFT' | 'RECRUITING' | 'TEAMED' | 'CLOSED'
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
