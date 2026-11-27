import { onScopeDispose, ref, watch } from 'vue'
import { useTeamSyncStore } from '@/stores/team-sync'
import { teamErrorMessage, teamErrorStatus } from '@/api/team-result'

/** 不重试写请求；成功与刷新反馈独立，失效会话不提示、不跳转。 */
export function useTeamAction(context: () => string) {
  const sync = useTeamSyncStore()
  const busy = ref(false),
    notice = ref(''),
    error = ref('')
  let generation = 0
  watch(
    [context, () => sync.sessionRevision],
    () => {
      generation++
      busy.value = false
      notice.value = error.value = ''
    },
    { flush: 'sync' },
  )
  onScopeDispose(() => {
    generation++
  })
  async function run<T>(id: number, operation: () => Promise<T>, message: (result: T) => string) {
    if (busy.value) return false
    const ticket = generation,
      session = sync.sessionRevision
    busy.value = true
    notice.value = error.value = ''
    try {
      const result = await operation()
      if (ticket !== generation) return false
      notice.value = message(result)
      if (!(await sync.afterTeamMutation(id, session)) || ticket !== generation) return false
      return true
    } catch (cause) {
      if (ticket === generation) {
        error.value = teamErrorMessage(cause)
        if ([403, 404, 409].includes(teamErrorStatus(cause) ?? 0)) sync.invalidateTeam(id, session)
      }
      return false
    } finally {
      if (ticket === generation) busy.value = false
    }
  }
  return { busy, notice, error, run }
}
