import { computed, onScopeDispose, ref, watch, type Ref } from 'vue'
import { listMessages, sendMessage, markConversationRead } from '@/api/conversations'
import { sessionToken } from '@/api/session'
import { useAuthStore } from '@/stores/auth'
import { useConversationsStore } from '@/stores/conversations'
import { useTeamSyncStore } from '@/stores/team-sync'
import { teamErrorMessage, teamErrorStatus } from '@/api/team-result'
import { createMessageDraft, mergeConversationMessages } from '@/utils/conversation-messages'
import type { ConversationMessage, SendMessageInput } from '@/types/conversation'

export interface OutgoingMessage {
  input: SendMessageInput
  state: 'sending' | 'failed'
  error: string
}
// 后端历史按 ID 升序分页：先定位末页，再向前翻历史；轮询游标只能由读取响应推进。
export function useConversation(id: Ref<number>, visible: Ref<boolean>) {
  const conversations = useConversationsStore(),
    auth = useAuthStore(),
    sync = useTeamSyncStore()
  const messages = ref<ConversationMessage[]>([]),
    outgoing = ref<OutgoingMessage[]>([])
  const loading = ref(false),
    historyLoading = ref(false),
    polling = ref(false),
    reading = ref(false)
  const error = ref(''),
    historyError = ref(''),
    pollError = ref(''),
    readError = ref('')
  const denied = ref(false),
    sendBlocked = ref(false),
    caughtUp = ref(false)
  const historyPage = ref(1)
  const conversation = computed(() => conversations.items.find((item) => item.id === id.value))
  const canSend = computed(
    () =>
      !loading.value &&
      !error.value &&
      !denied.value &&
      !sendBlocked.value &&
      conversation.value?.status === 'ACTIVE',
  )
  let epoch = 0,
    disposed = false,
    controller = new AbortController(),
    cursor: number | undefined,
    lastRead = 0
  let timer: ReturnType<typeof setTimeout> | undefined
  const pageSize = 50
  function active(run: number) {
    return run === epoch && !!sessionToken.value
  }
  function merge(rows: ConversationMessage[]) {
    messages.value = mergeConversationMessages(id.value, messages.value, rows)
    const ids = new Set(
      messages.value
        .filter((item) => item.senderId === auth.user?.id)
        .map((item) => item.clientMessageId),
    )
    outgoing.value = outgoing.value.filter((item) => !ids.has(item.input.clientMessageId))
  }
  function revoke(cause: unknown) {
    if (![401, 403, 404].includes(teamErrorStatus(cause) ?? 0)) return false
    denied.value = true
    messages.value = []
    outgoing.value = []
    error.value = teamErrorMessage(cause)
    clearTimeout(timer)
    controller.abort()
    conversations.remove(id.value)
    void conversations.refresh()
    return true
  }
  async function markVisibleRead() {
    if (
      !visible.value ||
      document.visibilityState !== 'visible' ||
      !caughtUp.value ||
      reading.value ||
      denied.value ||
      !cursor ||
      cursor <= lastRead ||
      !sessionToken.value
    )
      return
    const run = epoch,
      messageId = cursor,
      conversationId = id.value
    reading.value = true
    readError.value = ''
    try {
      const result = await markConversationRead(conversationId, messageId)
      if (!active(run)) return
      lastRead = Math.max(lastRead, result.lastReadMessageId ?? 0)
      conversations.updateUnread(conversationId, result.unreadCount)
    } catch (cause) {
      if (active(run)) {
        if (!revoke(cause)) readError.value = teamErrorMessage(cause)
      }
    } finally {
      if (active(run)) reading.value = false
    }
  }
  async function poll() {
    if (
      disposed ||
      polling.value ||
      loading.value ||
      denied.value ||
      error.value ||
      !sessionToken.value ||
      document.visibilityState !== 'visible'
    )
      return
    const run = epoch
    polling.value = true
    try {
      // 分批补齐，限制单轮请求数；达到上限下轮继续，不能直接跳到发送回执的 ID。
      for (let batch = 0; batch < 10; batch++) {
        if (document.visibilityState !== 'visible') break
        const result = await listMessages(
          id.value,
          { page: 1, pageSize: 100, ...(cursor ? { afterMessageId: cursor } : {}) },
          controller.signal,
        )
        if (!active(run)) return
        merge(result.records)
        const max = result.records.at(-1)?.id
        if (max) cursor = max
        caughtUp.value = !result.hasMore
        conversations.updateUnread(id.value, result.unreadCount)
        if (!result.hasMore) break
        if (!max) throw new Error('增量消息未推进，请刷新会话')
      }
      pollError.value = ''
      await markVisibleRead()
      if (active(run) && document.visibilityState === 'visible') void conversations.refresh()
    } catch (cause) {
      if (active(run) && !controller.signal.aborted && !revoke(cause))
        pollError.value = teamErrorMessage(cause)
    } finally {
      if (active(run)) polling.value = false
    }
  }
  function schedule() {
    clearTimeout(timer)
    if (disposed || denied.value || !sessionToken.value || document.visibilityState !== 'visible')
      return
    timer = setTimeout(async () => {
      await poll()
      schedule()
    }, 4000)
  }
  async function reload(preservePending = true) {
    const run = ++epoch
    clearTimeout(timer)
    controller.abort()
    controller = new AbortController()
    messages.value = []
    if (!preservePending) outgoing.value = []
    cursor = undefined
    lastRead = 0
    loading.value = true
    polling.value = false
    historyLoading.value = false
    reading.value = false
    error.value = ''
    historyError.value = ''
    pollError.value = ''
    readError.value = ''
    denied.value = false
    sendBlocked.value = false
    caughtUp.value = false
    historyPage.value = 1
    if (!Number.isSafeInteger(id.value) || id.value <= 0 || !sessionToken.value) {
      error.value = '会话编号无效或尚未登录。'
      loading.value = false
      return
    }
    try {
      if (!(await conversations.refresh())) throw new Error('会话列表加载失败')
      if (!active(run)) return
      if (!conversation.value) {
        denied.value = true
        error.value = '会话不存在或当前账号已无访问资格。'
        return
      }
      const first = await listMessages(id.value, { page: 1, pageSize }, controller.signal)
      if (!active(run)) return
      const lastPage = Math.max(1, Math.ceil(first.total / pageSize))
      const result =
        lastPage === 1
          ? first
          : await listMessages(id.value, { page: lastPage, pageSize }, controller.signal)
      if (!active(run)) return
      merge(result.records)
      historyPage.value = lastPage
      cursor = result.records.at(-1)?.id
      caughtUp.value = !result.hasMore
      conversations.updateUnread(id.value, result.unreadCount)
    } catch (cause) {
      if (active(run) && !controller.signal.aborted && !revoke(cause))
        error.value = teamErrorMessage(cause)
    } finally {
      if (active(run)) {
        loading.value = false
        schedule()
      }
    }
  }
  async function loadOlder() {
    if (historyLoading.value || historyPage.value <= 1 || denied.value) return
    const run = epoch
    historyLoading.value = true
    historyError.value = ''
    try {
      const result = await listMessages(
        id.value,
        { page: historyPage.value - 1, pageSize },
        controller.signal,
      )
      if (!active(run)) return
      merge(result.records)
      historyPage.value = result.page
    } catch (cause) {
      if (active(run) && !controller.signal.aborted && !revoke(cause))
        historyError.value = teamErrorMessage(cause)
    } finally {
      if (active(run)) historyLoading.value = false
    }
  }
  async function deliver(input: SendMessageInput) {
    if (!canSend.value || outgoing.value.some((item) => item.state === 'sending')) return
    const run = epoch
    let row = outgoing.value.find((item) => item.input.clientMessageId === input.clientMessageId)
    if (!row) {
      row = { input: { ...input }, state: 'sending', error: '' }
      outgoing.value.push(row)
    } else {
      row.state = 'sending'
      row.error = ''
    }
    try {
      const message = await sendMessage(id.value, { ...row.input })
      if (!active(run)) return
      merge([message])
      // 发送回执不推进读取游标，避免漏掉并发到达的其他消息。
      caughtUp.value = false
      void poll()
      void conversations.refresh()
    } catch (cause) {
      if (!active(run)) return
      const pending = outgoing.value.find(
        (item) => item.input.clientMessageId === input.clientMessageId,
      )
      if (pending) {
        pending.state = 'failed'
        pending.error = teamErrorMessage(cause)
      }
      if (teamErrorStatus(cause) === 403) {
        sendBlocked.value = true
        void poll()
      }
      if (teamErrorStatus(cause) === 409) {
        void conversations.refresh()
      }
    }
  }
  function send(content: string, sourceRecruitmentId?: number) {
    return deliver(createMessageDraft(content, sourceRecruitmentId))
  }
  function visibilityChanged() {
    clearTimeout(timer)
    if (document.visibilityState === 'visible') {
      void poll().finally(schedule)
    }
  }
  document.addEventListener('visibilitychange', visibilityChanged)
  watch(
    [id, sessionToken],
    () => {
      void reload(false)
    },
    { immediate: true },
  )
  watch(visible, (value) => {
    if (value) void markVisibleRead()
  })
  watch(
    () => sync.conversationsRevision,
    () => {
      const run = epoch
      void conversations.refresh().then(() => {
        if (!active(run)) return
        if (!conversation.value && conversations.loaded && !conversations.error) {
          denied.value = true
          error.value = '成员或会话资格已变化，请返回会话列表。'
          messages.value = []
          outgoing.value = []
          clearTimeout(timer)
          controller.abort()
        }
      })
    },
  )
  onScopeDispose(() => {
    disposed = true
    epoch++
    controller.abort()
    clearTimeout(timer)
    document.removeEventListener('visibilitychange', visibilityChanged)
  })
  return {
    messages,
    outgoing,
    loading,
    historyLoading,
    polling,
    reading,
    error,
    historyError,
    pollError,
    readError,
    denied,
    sendBlocked,
    caughtUp,
    conversation,
    canSend,
    historyPage,
    reload,
    poll,
    loadOlder,
    send,
    retry: (row: OutgoingMessage) => deliver(row.input),
    markVisibleRead,
  }
}
