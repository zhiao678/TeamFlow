<script setup lang="ts">
import { ref, watch, onScopeDispose } from 'vue'
import { useRouter } from 'vue-router'
import { getTeamConversation } from '@/api/conversations'
import { teamErrorMessage } from '@/api/team-result'
import { teamRoutePath, teamRoutes } from '@/router/team-routes'
import { useTeamSyncStore } from '@/stores/team-sync'
import { isTeamConversation } from '@/utils/team-member-side'
const props = defineProps<{ recruitmentId: number; readOnly?: boolean }>()
const router = useRouter(),
  sync = useTeamSyncStore()
const busy = ref(false),
  error = ref('')
let controller: AbortController | undefined
watch([() => props.recruitmentId, () => sync.sessionRevision], () => {
  controller?.abort()
  busy.value = false
  error.value = ''
})
onScopeDispose(() => controller?.abort())
async function enter() {
  if (busy.value || !router.hasRoute(teamRoutes.conversation.name)) return
  controller?.abort()
  const current = new AbortController(),
    session = sync.sessionRevision,
    id = props.recruitmentId
  controller = current
  busy.value = true
  error.value = ''
  try {
    const conversation = await getTeamConversation(id, current.signal)
    if (current.signal.aborted || session !== sync.sessionRevision || id !== props.recruitmentId)
      return
    if (!isTeamConversation(conversation, id)) throw new Error('无效队伍会话')
    await router.push(teamRoutePath('conversation', conversation.id))
  } catch (cause) {
    if (!current.signal.aborted && session === sync.sessionRevision) {
      error.value = teamErrorMessage(cause)
      sync.invalidateTeam(id, session)
    }
  } finally {
    if (!current.signal.aborted) busy.value = false
  }
}
</script>
<template>
  <div>
    <button v-if="router.hasRoute(teamRoutes.conversation.name)" :disabled="busy" @click="enter">
      {{ busy ? '正在确认群权限…' : readOnly ? '查看群聊记录' : '进入队伍群' }}
    </button>
    <p v-else>聊天入口待接入。</p>
    <p v-if="error" role="alert" class="form-error">{{ error }}</p>
  </div>
</template>
