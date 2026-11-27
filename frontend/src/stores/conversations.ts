import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken } from '@/api/session'
import { listConversations } from '@/api/conversations'
import { teamErrorMessage } from '@/api/team-result'
import type { Conversation } from '@/types/conversation'

export const useConversationsStore = defineStore('conversations', () => {
  const items = ref<Conversation[]>([])
  const loaded = ref(false),
    loading = ref(false),
    error = ref('')
  let version = 0
  let controller: AbortController | undefined
  let pending: Promise<boolean> | null = null
  const unreadCount = computed(() =>
    loaded.value && !error.value
      ? items.value.reduce((sum, item) => sum + item.unreadCount, 0)
      : null,
  )
  function invalidate() {
    version++
    controller?.abort()
    pending = null
    loading.value = false
  }
  watch(
    sessionToken,
    () => {
      invalidate()
      items.value = []
      loaded.value = false
      error.value = ''
    },
    { flush: 'sync' },
  )
  function refresh(): Promise<boolean> {
    if (!sessionToken.value) return Promise.resolve(false)
    if (pending) return pending
    const current = ++version
    controller = new AbortController()
    const signal = controller.signal
    loading.value = true
    pending = (async () => {
      try {
        const rows = await listConversations(signal)
        if (current !== version) return false
        items.value = rows
        loaded.value = true
        error.value = ''
        return true
      } catch (cause) {
        if (current === version && !signal.aborted) error.value = teamErrorMessage(cause)
        return false
      } finally {
        if (current === version) {
          loading.value = false
          pending = null
        }
      }
    })()
    return pending
  }
  function updateUnread(id: number, count: number) {
    invalidate()
    const row = items.value.find((item) => item.id === id)
    if (row) row.unreadCount = count
  }
  function remove(id: number) {
    invalidate()
    items.value = items.value.filter((item) => item.id !== id)
  }
  return { items, loaded, loading, error, unreadCount, refresh, updateUnread, remove }
})
