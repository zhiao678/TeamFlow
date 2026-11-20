// 本地受控接口契约与 Vue effectScope 回归；不是实际后端联调。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
import { effectScope, ref, nextTick } from 'vue'
const { request } = await import('../src/api/request.ts')
const { useAuthStore } = await import('../src/stores/auth.ts')
const { useConversationsStore } = await import('../src/stores/conversations.ts')
const { useConversation } = await import('../src/composables/useConversation.ts')
const { notificationTarget } = await import('../src/utils/notification-target.ts')
const axios = (await import('axios')).default
globalThis.document = new EventTarget()
document.visibilityState = 'visible'
const auth = useAuthStore()
auth.accept({ token: 'chat-contract', user: { id: 7, displayName: '测试', skills: [] } })
const conversations = useConversationsStore()
let denied = false,
  loseReply = false,
  blockSend = false,
  holdRead,
  heldReadArrived
let writes = [],
  reads = [],
  queries = []
const makeMessage = (id, conversationId = 12) => ({
  id,
  conversationId,
  senderId: 8,
  senderDisplayName: '队友',
  clientMessageId: `seed-${id}`,
  content: `消息${id}`,
  sourceRecruitmentId: null,
  status: 'VISIBLE',
  createdAt: '2026-11-20T10:00:00',
})
let messages = Array.from({ length: 101 }, (_, i) => makeMessage(i + 1))
const rows = [
  { id: 12, type: 'TEAM', recruitmentId: 3, status: 'ACTIVE', unreadCount: 101 },
  { id: 13, type: 'DIRECT', otherUserDisplayName: '同学', status: 'READ_ONLY', unreadCount: 0 },
]
let lastRead = 0
request.defaults.adapter = async (config) => {
  const body = config.data ? JSON.parse(config.data) : null
  const response = (data, status = 200) => ({
    data: { code: status === 200 ? 0 : status * 100, message: 'contract', data },
    status,
    statusText: '',
    headers: {},
    config,
  })
  const reject = (status) => {
    throw new axios.AxiosError('contract', undefined, config, undefined, response(null, status))
  }
  if (config.url === '/conversations')
    return response(rows.filter((row) => !denied || row.id !== 12).map((row) => ({ ...row })))
  const id = Number(config.url.split('/')[2])
  if (config.url.endsWith('/read')) {
    reads.push(body.lastReadMessageId)
    lastRead = Math.max(lastRead, body.lastReadMessageId)
    return response({
      conversationId: id,
      lastReadMessageId: lastRead,
      unreadCount: messages.filter((item) => item.id > lastRead).length,
    })
  }
  if (id === 12 && denied) return reject(403)
  if (config.method === 'post') {
    writes.push(body)
    if (blockSend) return reject(403)
    let saved = messages.find((item) => item.clientMessageId === body.clientMessageId)
    if (!saved) {
      saved = { ...makeMessage((messages.at(-1)?.id ?? 0) + 1, id), ...body, senderId: 7 }
      messages.push(saved)
    }
    if (loseReply) {
      loseReply = false
      throw new axios.AxiosError('lost reply', 'ECONNABORTED', config)
    }
    return response(saved)
  }
  queries.push({ id, ...config.params })
  if (heldReadArrived && config.params.afterMessageId) {
    heldReadArrived()
    await new Promise((resolve) => {
      holdRead = resolve
    })
  }
  const all = messages.filter(
    (item) =>
      item.conversationId === id &&
      (!config.params.afterMessageId || item.id > config.params.afterMessageId),
  )
  const page = config.params.page ?? 1,
    size = config.params.pageSize ?? 50
  return response({
    records: all.slice((page - 1) * size, page * size),
    page,
    pageSize: size,
    total: all.length,
    hasMore: page * size < all.length,
    unreadCount: all.filter((item) => item.id > lastRead).length,
  })
}
const scope = effectScope(),
  id = ref(12),
  visible = ref(false)
const chat = scope.run(() => useConversation(id, visible))
async function until(test) {
  for (let i = 0; i < 200; i++) {
    if (test()) return
    await new Promise((resolve) => setTimeout(resolve, 5))
  }
  throw new Error('等待聊天状态超时')
}
try {
  await until(() => !chat.loading.value)
  assert.equal(chat.error.value, '')
  assert.equal(chat.historyPage.value, 3)
  assert.deepEqual(
    chat.messages.value.map((item) => item.id),
    [101],
    '升序接口应先定位最新页',
  )
  assert.equal(reads.length, 0, '未阅读视口不推进已读')
  await chat.loadOlder()
  assert.equal(chat.messages.value[0].id, 51)
  const beforeHidden = queries.length
  document.visibilityState = 'hidden'
  await chat.poll()
  await chat.markVisibleRead()
  assert.equal(queries.length, beforeHidden, '隐藏页面不发起轮询')
  assert.equal(reads.length, 0, '隐藏页面不推进已读')
  document.visibilityState = 'visible'
  for (let i = 102; i <= 351; i++) messages.push(makeMessage(i))
  await chat.poll()
  assert.equal(chat.messages.value.at(-1).id, 351)
  assert.deepEqual(
    queries.filter((q) => q.afterMessageId).map((q) => q.afterMessageId),
    [101, 201, 301],
    'hasMore 必须连续补齐批次',
  )
  visible.value = true
  await until(() => reads.includes(351))
  await until(() => !chat.reading.value)
  messages.push(makeMessage(352))
  loseReply = true
  await chat.send('网络重试测试')
  assert.equal(chat.outgoing.value[0].state, 'failed')
  const original = { ...chat.outgoing.value[0].input }
  await chat.retry(chat.outgoing.value[0])
  await until(() => !chat.polling.value)
  assert.deepEqual(writes[0], writes[1], '重试必须复用 ID 与原正文')
  assert.deepEqual(writes[1], original)
  assert.ok(
    chat.messages.value.some((item) => item.id === 352),
    '发送回执不能跳过并发消息',
  )
  assert.equal(
    chat.messages.value.filter((item) => item.clientMessageId === original.clientMessageId).length,
    1,
  )
  assert.equal(chat.outgoing.value.length, 0)
  blockSend = true
  await chat.send('被屏蔽时不能假成功')
  await until(() => !chat.polling.value)
  assert.equal(chat.sendBlocked.value, true)
  assert.equal(chat.outgoing.value[0].state, 'failed')
  blockSend = false
  await chat.reload()
  assert.equal(chat.outgoing.value.length, 1, '手动刷新保留失败草稿')
  visible.value = false
  const arrived = new Promise((resolve) => {
    heldReadArrived = resolve
  })
  const oldPoll = chat.poll()
  await arrived
  id.value = 13
  await nextTick()
  heldReadArrived = undefined
  holdRead()
  await oldPoll
  await until(() => !chat.loading.value)
  assert.equal(chat.messages.value.length, 0, '切换会话隔离迟到响应')
  assert.equal(chat.outgoing.value.length, 0)
  assert.equal(chat.canSend.value, false, '只读会话不允许发送')
  id.value = 12
  await nextTick()
  await until(() => !chat.loading.value)
  denied = true
  await chat.poll()
  assert.equal(chat.denied.value, true)
  assert.equal(chat.messages.value.length, 0)
  await until(() => !conversations.loading)
  assert.ok(!conversations.items.some((item) => item.id === 12), '撤销资格移除失效会话')
  const item = { targetType: 'LEADER_TRANSFER', leaderTransferId: 9 }
  assert.equal(notificationTarget(item, []).path, null)
  assert.equal(notificationTarget(item, ['/my-leader-transfers']).query.transferId, '9')
  assert.equal(
    notificationTarget(
      { eventType: 'MEMBER_EXIT_REQUESTED', targetType: 'RECRUITMENT', recruitmentId: 3 },
      ['/my-recruitments/:id/team-management'],
    ).path,
    '/my-recruitments/3/team-management',
  )
  auth.clear()
  assert.deepEqual(conversations.items, [])
  assert.equal(conversations.unreadCount, null)
  console.log(
    'PASS: 最新历史、补齐轮询、视口已读、幂等重试、并发不漏消息、切换隔离、权限撤销及通知路由',
  )
} finally {
  scope.stop()
  auth.clear()
}
