<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useTeamRead } from '@/composables/useTeamRead'
import { getRecruitment } from '@/api/recruitment-query'
import { listRecruitmentMembers } from '@/api/members'
import { getTeamConversation } from '@/api/conversations'
import { teamErrorStatus } from '@/api/team-result'
import { memberRoleLabels } from '@/types/application'
import TeamConversationEntry from './TeamConversationEntry.vue'
const props = defineProps<{ recruitmentId: number }>()
const auth = useAuthStore(),
  sync = useTeamSyncStore()
const { data, loading, error, reload } = useTeamRead(
  () => `${props.recruitmentId}:${sync.revisions[props.recruitmentId] ?? 0}`,
  async (signal) => {
    const recruitment = await getRecruitment(props.recruitmentId, signal)
    const members = await listRecruitmentMembers(props.recruitmentId, signal)
    let conversation = null
    try {
      conversation = await getTeamConversation(props.recruitmentId, signal)
    } catch (cause) {
      if (![403, 404].includes(teamErrorStatus(cause) ?? 0)) throw cause
    }
    return { recruitment, member: members.find((row) => row.id === auth.user?.id), conversation }
  },
)
</script>
<template>
  <section class="panel recruit-card">
    <h2>处理后的队伍状态</h2>
    <p v-if="loading" role="status">正在重新查询负责人、成员与群聊资格…</p>
    <div v-else-if="error" role="alert">
      <p>操作结果已保留，但队伍状态刷新失败。{{ error }}</p>
      <button @click="reload">重试队伍状态</button>
    </div>
    <template v-else-if="data"
      ><p>{{ data.recruitment.title }} · 负责人：{{ data.recruitment.ownerDisplayName }}</p>
      <p>
        {{ data.recruitment.currentMembers }} / {{ data.recruitment.maxMembers }} 人 · 我的身份：{{
          data.member ? memberRoleLabels[data.member.role] : '非成员'
        }}
      </p>
      <TeamConversationEntry
        v-if="data.conversation"
        :recruitment-id="recruitmentId"
        :read-only="data.conversation.status === 'READ_ONLY'"
      />
      <p v-else>当前没有可访问的队伍群。</p></template
    >
  </section>
</template>
