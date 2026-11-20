import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken } from '@/api/session'
import { useNotificationsStore } from './notifications'

export const useTeamSyncStore = defineStore('team-sync', () => {
  const revisions = ref<Record<number, number>>({})
  const conversationsRevision = ref(0)
  const sessionRevision = ref(0)
  watch(
    sessionToken,
    () => {
      revisions.value = {}
      conversationsRevision.value = 0
      sessionRevision.value++
    },
    { flush: 'sync' },
  )
  function invalidateTeam(recruitmentId: number, expectedSession: number) {
    if (!sessionToken.value || expectedSession !== sessionRevision.value) return false
    if (!Number.isSafeInteger(recruitmentId) || recruitmentId <= 0)
      throw new Error('招募编号必须为正整数')
    revisions.value[recruitmentId] = (revisions.value[recruitmentId] ?? 0) + 1
    conversationsRevision.value++
    return true
  }
  /** 写请求成功后调用；只广播重新查询信号，不伪造新人数或权限，不代替页面请求。 */
  async function afterTeamMutation(recruitmentId: number, expectedSession: number) {
    if (!invalidateTeam(recruitmentId, expectedSession)) return false
    await useNotificationsStore().refreshUnreadCount()
    return expectedSession === sessionRevision.value && !!sessionToken.value
  }
  return { revisions, conversationsRevision, sessionRevision, invalidateTeam, afterTeamMutation }
})
