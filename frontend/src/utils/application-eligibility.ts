import type { Application } from '@/types/application'
import { displayStatus, type Recruitment } from '@/types/recruitment'
export function latestApplication(items: Application[], recruitmentId: number) {
  return items
    .filter((item) => item.recruitmentId === recruitmentId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id)[0]
}
export function applicationBlock(
  item: Recruitment,
  userId: number,
  memberIds: number[],
  latest: Application | undefined,
  now: number,
) {
  if (item.ownerId === userId) return '你是招募负责人，无需申请。'
  if (memberIds.includes(userId)) return '你已是该队伍的正式成员。'
  if (latest?.status === 'PENDING') return '已有待处理申请，请等待结果或先撤回。'
  const status = displayStatus(item, now)
  return status === '招募中' ? '' : `当前招募${status}，不能提交申请。`
}
