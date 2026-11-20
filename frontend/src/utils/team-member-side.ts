import type { LeaderTransfer } from '@/types/team-management'
import type { Conversation } from '@/types/conversation'

export function canRespondToTransfer(row: LeaderTransfer, userId?: number) {
  return row.status === 'PENDING' && row.toUserId === userId
}
export function transferEffect(row: LeaderTransfer) {
  return row.exitAfterTransfer
    ? '接受后你将成为负责人；原负责人将退出队伍并失去群聊资格。'
    : '接受后你将成为负责人；原负责人继续作为普通成员留队。'
}
export function isTeamConversation(row: Conversation, recruitmentId: number) {
  return (
    Number.isSafeInteger(recruitmentId) &&
    recruitmentId > 0 &&
    row.type === 'TEAM' &&
    row.recruitmentId === recruitmentId &&
    Number.isSafeInteger(row.id) &&
    row.id > 0
  )
}
/** 每个 worker 一次只查询一个队伍，取消后不再领取新任务。 */
export async function mapTeams<T, R>(
  rows: T[],
  signal: AbortSignal,
  fetcher: (row: T) => Promise<R>,
  limit = 3,
) {
  const result: R[] = []
  let next = 0
  await Promise.all(
    Array.from({ length: Math.min(limit, rows.length) }, async () => {
      while (!signal.aborted) {
        const index = next++
        if (index >= rows.length) return
        result[index] = await fetcher(rows[index]!)
      }
    }),
  )
  return result
}
