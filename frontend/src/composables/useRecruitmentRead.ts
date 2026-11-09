import { ref, shallowRef, watch, onScopeDispose } from 'vue'
import { sessionToken } from '@/api/session'
import { browseError } from '@/api/recruitment-query'
export function useRecruitmentRead<T>(
  key: () => string,
  fetcher: (key: string, signal: AbortSignal) => Promise<T>,
) {
  const data = shallowRef<T>()
  const error = ref('')
  const loading = ref(true)
  const attempts = ref(0)
  const retry = () => {
    attempts.value++
  }
  watch(
    [key, attempts, sessionToken],
    async ([value], _, cleanup) => {
      const ctl = new AbortController()
      cleanup(() => ctl.abort())
      data.value = undefined
      error.value = ''
      loading.value = true
      try {
        const result = await fetcher(value, ctl.signal)
        if (!ctl.signal.aborted) data.value = result
      } catch (cause) {
        if (!ctl.signal.aborted) error.value = browseError(cause)
      } finally {
        if (!ctl.signal.aborted) loading.value = false
      }
    },
    { immediate: true },
  )
  return { data, loading, error, retry }
}
export function useBrowseClock() {
  const now = ref(Date.now())
  const timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
  onScopeDispose(() => window.clearInterval(timer))
  return now
}
