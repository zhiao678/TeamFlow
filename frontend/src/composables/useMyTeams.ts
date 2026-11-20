import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { listConversations } from '@/api/conversations'
import { getRecruitment } from '@/api/recruitment-query'
import { listRecruitmentMembers } from '@/api/members'
import { teamErrorMessage, teamErrorStatus } from '@/api/team-result'
import type { Conversation } from '@/types/conversation'
import type { Recruitment } from '@/types/recruitment'
import type { TeamMember } from '@/types/application'
import { isTeamConversation, mapTeams } from '@/utils/team-member-side'
import { useTeamRead } from './useTeamRead'

export interface MyTeamRow {
  conversation: Conversation
  recruitment?: Recruitment
  member?: TeamMember
  error?: string
  revoked?: boolean
}
export function useMyTeams() {
  const auth = useAuthStore(),
    sync = useTeamSyncStore()
  return useTeamRead(
    () => `${sync.conversationsRevision}:${auth.user?.id ?? 0}`,
    async (signal) => {
      const userId = auth.user?.id
      if (!userId) throw new Error('身份尚未确认')
      const conversations = (await listConversations(signal)).filter((row) => row.type === 'TEAM')
      const rows = await mapTeams(
        conversations,
        signal,
        async (conversation): Promise<MyTeamRow> => {
          const id = conversation.recruitmentId
          if (id === null || !isTeamConversation(conversation, id))
            return { conversation, error: '队伍会话缺少有效招募编号，请刷新。' }
          try {
            // 串行读取，最多同时存在三个队伍请求。
            const recruitment = await getRecruitment(id, signal)
            const members = await listRecruitmentMembers(id, signal)
            const member = members.find((row) => row.id === userId)
            return { conversation, recruitment, member, revoked: !member }
          } catch (cause) {
            return {
              conversation,
              error: teamErrorMessage(cause),
              revoked: [403, 404].includes(teamErrorStatus(cause) ?? 0),
            }
          }
        },
      )
      if (signal.aborted) return { rows: [], removed: false }
      const revoked = rows.filter((row) => row.revoked)
      if (!revoked.length) return { rows, removed: false }
      // 资格撤销时再查有效会话；不无限重试，也不保留旧群入口。
      const fresh = await listConversations(signal)
      return {
        rows: rows.filter(
          (row) => !row.revoked && fresh.some((item) => item.id === row.conversation.id),
        ),
        removed: true,
      }
    },
  )
}
