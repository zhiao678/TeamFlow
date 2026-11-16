import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken } from '@/api/session'
import {
  listMyApplications,
  submitApplication,
  withdrawApplication,
  applicantMessage,
  applicationStatus,
} from '@/api/applications'
import { useNotificationsStore } from './notifications'
import type { Application, ApplicationInput } from '@/types/application'

export const useApplicationsStore = defineStore('applications', () => {
  const items = ref<Application[]>([])
  const loading = ref(false),
    ready = ref(false),
    busy = ref(false)
  const error = ref(''),
    actionError = ref(''),
    notice = ref('')
  let generation = 0,
    version = 0
  let controller: AbortController | undefined
  watch(
    sessionToken,
    () => {
      generation++
      version++
      controller?.abort()
      items.value = []
      loading.value = ready.value = busy.value = false
      error.value = actionError.value = notice.value = ''
    },
    { flush: 'sync' },
  )
  async function load() {
    if (!sessionToken.value) return false
    const ticket = ++version,
      account = generation
    controller?.abort()
    const current = new AbortController()
    controller = current
    loading.value = true
    ready.value = false
    error.value = ''
    try {
      const rows = await listMyApplications(current.signal)
      if (ticket !== version || account !== generation) return false
      items.value = rows
      ready.value = true
      return true
    } catch (cause) {
      if (ticket === version && account === generation) error.value = applicantMessage(cause)
      return false
    } finally {
      if (ticket === version && account === generation) loading.value = false
    }
  }
  async function act(operation: () => Promise<Application>, success: string) {
    if (!sessionToken.value || busy.value) return false
    const account = generation
    busy.value = true
    actionError.value = notice.value = ''
    version++
    controller?.abort()
    ready.value = false
    loading.value = false
    try {
      await operation()
      if (account !== generation) return false
      notice.value = success
      void useNotificationsStore().refreshUnreadCount()
      const refreshed = await load()
      if (account !== generation) return false
      if (!refreshed) notice.value += ' 状态刷新失败，请重试刷新，无需重复提交。'
      return true
    } catch (cause) {
      if (account !== generation) return false
      actionError.value = applicantMessage(cause)
      await load()
      return account === generation && applicationStatus(cause) === 409
    } finally {
      if (account === generation) busy.value = false
    }
  }
  return {
    items,
    loading,
    ready,
    busy,
    error,
    actionError,
    notice,
    load,
    submit: (id: number, input: ApplicationInput) =>
      act(() => submitApplication(id, input), '申请已提交，请等待负责人处理。'),
    withdraw: (id: number) => act(() => withdrawApplication(id), '申请已撤回。'),
  }
})
