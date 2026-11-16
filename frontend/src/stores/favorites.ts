import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken } from '@/api/session'
import { setFavorite } from '@/api/favorites'
import { applicantMessage } from '@/api/applications'
import type { Recruitment } from '@/types/recruitment'

export const useFavoritesStore = defineStore('favorites', () => {
  const states = ref<Record<number, boolean>>({})
  const busy = ref<Record<number, boolean>>({})
  const errors = ref<Record<number, string>>({})
  let generation = 0
  let revision = 0
  let changed: Record<number, number> = {}
  watch(
    sessionToken,
    () => {
      generation++
      states.value = {}
      busy.value = {}
      errors.value = {}
      changed = {}
    },
    { flush: 'sync' },
  )

  // 读取开始时取票据，避免旧列表响应覆盖刚完成的收藏操作。
  function reader() {
    const ticket = revision,
      account = generation
    return (rows: Recruitment[]) => {
      if (account !== generation) return
      for (const row of rows) {
        if (!busy.value[row.id] && (changed[row.id] ?? 0) <= ticket)
          states.value[row.id] = !!sessionToken.value && row.favorited
      }
    }
  }
  async function toggle(id: number, fallback = false) {
    if (!sessionToken.value || busy.value[id]) return false
    const account = generation
    const next = !(states.value[id] ?? fallback)
    busy.value[id] = true
    errors.value[id] = ''
    changed[id] = ++revision
    try {
      await setFavorite(id, next)
      if (account !== generation) return false
      states.value[id] = next
      return true
    } catch (cause) {
      if (account === generation) errors.value[id] = applicantMessage(cause)
      return false
    } finally {
      if (account === generation) {
        busy.value[id] = false
        changed[id] = ++revision
      }
    }
  }
  return { states, busy, errors, reader, toggle }
})
