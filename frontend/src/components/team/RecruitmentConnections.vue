<script setup lang="ts">
import { ref, watch, onScopeDispose } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useTeamRead } from '@/composables/useTeamRead'
import { createPrivateConversation, getTeamConversation } from '@/api/conversations'
import { teamErrorMessage, teamErrorStatus } from '@/api/team-result'
import { teamRoutePath, teamRoutes } from '@/router/team-routes'
import { isTeamConversation } from '@/utils/team-member-side'
import type { Recruitment } from '@/types/recruitment'
import TeamConversationEntry from './TeamConversationEntry.vue'
const props = defineProps<{ item: Recruitment }>()
const auth = useAuthStore(),
  sync = useTeamSyncStore(),
  router = useRouter(),
  route = useRoute()
const busy = ref(false),
  contactError = ref('')
let generation = 0
watch(
  [() => props.item.id, () => sync.sessionRevision],
  () => {
    generation++
    busy.value = false
    contactError.value = ''
  },
  { flush: 'sync' },
)
onScopeDispose(() => {
  generation++
})
const {
  data: group,
  loading,
  error,
  reload,
} = useTeamRead(
  () => `${props.item.id}:${sync.revisions[props.item.id] ?? 0}`,
  async (signal) => {
    if (!auth.token) return { conversation: null, message: '登录后可查询队伍群访问资格。' }
    try {
      const conversation = await getTeamConversation(props.item.id, signal)
      if (!isTeamConversation(conversation, props.item.id)) throw new Error('无效群聊')
      return {
        conversation,
        message: conversation.status === 'READ_ONLY' ? '队伍群当前只读，可以查看历史消息。' : '',
      }
    } catch (cause) {
      if (teamErrorStatus(cause) === 403)
        return { conversation: null, message: '你当前没有该队伍群的访问资格。' }
      if (teamErrorStatus(cause) === 404)
        return { conversation: null, message: '该招募尚无可访问的队伍群。' }
      throw cause
    }
  },
)
async function contact() {
  if (!auth.token) {
    await router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (
    busy.value ||
    !auth.user ||
    auth.user.id === props.item.ownerId ||
    !router.hasRoute(teamRoutes.conversation.name)
  )
    return
  const ticket = generation,
    item = props.item
  busy.value = true
  contactError.value = ''
  try {
    const conversation = await createPrivateConversation(item.ownerId)
    if (ticket !== generation) return
    if (
      conversation.type !== 'DIRECT' ||
      !Number.isSafeInteger(conversation.id) ||
      conversation.id <= 0
    )
      throw new Error('无效私聊')
    await router.push({
      path: teamRoutePath('conversation', conversation.id),
      query: item.status === 'DRAFT' ? {} : { sourceRecruitmentId: String(item.id) },
    })
  } catch (cause) {
    if (ticket === generation)
      contactError.value =
        teamErrorStatus(cause) === 403
          ? '无法发起私聊，可能存在屏蔽或访问限制。'
          : teamErrorMessage(cause)
  } finally {
    if (ticket === generation) busy.value = false
  }
}
</script>
<template>
  <section class="team-connections">
    <h2>队伍沟通</h2>
    <p v-if="!router.hasRoute(teamRoutes.conversation.name)">聊天入口待接入。</p>
    <template v-else>
      <button
        v-if="!auth.token || (auth.user && auth.user.id !== item.ownerId)"
        :disabled="busy"
        @click="contact"
      >
        {{ busy ? '正在连接…' : '联系负责人' }}
      </button>
      <p v-if="contactError" role="alert" class="form-error">{{ contactError }}</p>
      <p v-if="loading" role="status">正在确认队伍群权限…</p>
      <div v-else-if="error" role="alert">
        <p>{{ error }}</p>
        <button @click="reload">重试群入口</button>
      </div>
      <template v-else-if="group"
        ><p v-if="group.message">{{ group.message }}</p>
        <TeamConversationEntry
          v-if="group.conversation"
          :recruitment-id="item.id"
          :read-only="group.conversation.status === 'READ_ONLY'"
      /></template>
    </template>
  </section>
</template>
