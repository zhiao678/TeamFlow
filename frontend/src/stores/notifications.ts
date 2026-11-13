import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken } from '@/api/session'
import {
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  notificationError,
} from '@/api/notifications'
import type { Notification, NotificationQuery } from '@/types/notification'

export const useNotificationsStore = defineStore('notifications', () => {
  const items = ref<Notification[]>([])
  const page = ref(1),
    pageSize = ref(20),
    total = ref(0)
  const unreadOnly = ref(false)
  const unreadCount = ref<number | null>(null)
  const loading = ref(false),
    countLoading = ref(false),
    marking = ref(false)
  const error = ref(''),
    countError = ref(''),
    actionError = ref(''),
    notice = ref('')
  let generation = 0,
    listVersion = 0,
    countVersion = 0
  let listController: AbortController | undefined
  let countController: AbortController | undefined
  function reset() {
    generation++
    listVersion++
    countVersion++
    listController?.abort()
    countController?.abort()
    items.value = []
    page.value = 1
    pageSize.value = 20
    total.value = 0
    unreadOnly.value = false
    unreadCount.value = null
    loading.value = false
    countLoading.value = false
    marking.value = false
    error.value = ''
    countError.value = ''
    actionError.value = ''
    notice.value = ''
  }
  watch(sessionToken, reset, { flush: 'sync' })
  function query(): NotificationQuery {
    return { page: page.value, pageSize: pageSize.value, unreadOnly: unreadOnly.value }
  }
  async function load(params: NotificationQuery = query()): Promise<boolean> {
    if (!sessionToken.value) return false
    const version = ++listVersion,
      current = generation,
      countTicket = ++countVersion
    listController?.abort()
    countController?.abort()
    countLoading.value = false
    const controller = new AbortController()
    listController = controller
    loading.value = true
    error.value = ''
    try {
      const result = await listNotifications(params, controller.signal)
      if (current !== generation || version !== listVersion) return false
      const lastPage = Math.max(1, Math.ceil(result.total / result.pageSize))
      if (result.page > lastPage) return await load({ ...params, page: lastPage })
      items.value = result.records
      page.value = result.page
      pageSize.value = result.pageSize
      total.value = result.total
      unreadOnly.value = params.unreadOnly
      if (countTicket === countVersion) {
        unreadCount.value = result.unreadCount
        countError.value = ''
      }
      return true
    } catch (cause) {
      if (current === generation && version === listVersion && !controller.signal.aborted)
        error.value = notificationError(cause)
      return false
    } finally {
      if (current === generation && version === listVersion) loading.value = false
    }
  }
  // 业务模块在申请/撤回/审核完成后可调用；只取第一页的全局 unreadCount。
  async function refreshUnreadCount() {
    if (!sessionToken.value) return
    const version = ++countVersion,
      current = generation
    countController?.abort()
    const controller = new AbortController()
    countController = controller
    countLoading.value = true
    try {
      const result = await listNotifications(
        { page: 1, pageSize: 1, unreadOnly: false },
        controller.signal,
      )
      if (current === generation && version === countVersion) {
        unreadCount.value = result.unreadCount
        countError.value = ''
      }
    } catch (cause) {
      if (current === generation && version === countVersion && !controller.signal.aborted) {
        countError.value = notificationError(cause)
        unreadCount.value = null
      }
    } finally {
      if (current === generation && version === countVersion) countLoading.value = false
    }
  }
  async function markRead(id?: number) {
    if (marking.value || !sessionToken.value) return false
    const current = generation
    marking.value = true
    actionError.value = ''
    notice.value = ''
    // 作废写操作之前发出的读取，防止旧响应恢复未读状态。
    ++listVersion
    ++countVersion
    listController?.abort()
    countController?.abort()
    loading.value = false
    countLoading.value = false
    try {
      if (id === undefined) await markAllNotificationsRead()
      else await markNotificationRead(id)
      if (current !== generation) return false
      notice.value = id === undefined ? '全部通知已标为已读。' : '通知已标为已读。'
      const refreshed = await load()
      if (current !== generation) return false
      if (!refreshed) {
        unreadCount.value = null
        notice.value += ' 列表刷新失败，请重试刷新，无需重复标记。'
      }
      return true
    } catch (cause) {
      if (current === generation) actionError.value = notificationError(cause)
      return false
    } finally {
      if (current === generation) marking.value = false
    }
  }
  return {
    items,
    page,
    pageSize,
    total,
    unreadOnly,
    unreadCount,
    loading,
    countLoading,
    marking,
    error,
    countError,
    actionError,
    notice,
    reset,
    load,
    refreshUnreadCount,
    markRead,
    markAllRead: () => markRead(),
  }
})
