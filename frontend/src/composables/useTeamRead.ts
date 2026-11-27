import { onScopeDispose, ref, shallowRef, watch } from 'vue'
import { useTeamSyncStore } from '@/stores/team-sync'
import { teamErrorMessage, teamErrorStatus } from '@/api/team-result'

/** 页面范围内读取：刷新立即撤下旧权限，换账号/路由和卸载后忽略迟到响应。 */
export function useTeamRead<T>(key: () => string, fetcher: (signal: AbortSignal) => Promise<T>) {
  const sync = useTeamSyncStore()
  const data = shallowRef<T>()
  const loading = ref(false),
    error = ref(''),
    status = ref<number>()
  let controller: AbortController | undefined
  let disposed = false
  async function reload() {
    if (disposed) return false
    controller?.abort()
    const current = new AbortController()
    controller = current
    data.value = undefined
    error.value = ''
    status.value = undefined
    loading.value = true
    try {
      const result = await fetcher(current.signal)
      if (current.signal.aborted) return false
      data.value = result
      return true
    } catch (cause) {
      if (!current.signal.aborted) {
        error.value = teamErrorMessage(cause)
        status.value = teamErrorStatus(cause)
      }
      return false
    } finally {
      if (!current.signal.aborted) loading.value = false
    }
  }
  watch([key, () => sync.sessionRevision], reload, { immediate: true, flush: 'sync' })
  onScopeDispose(() => {
    disposed = true
    controller?.abort()
  })
  return { data, loading, error, status, reload }
}
