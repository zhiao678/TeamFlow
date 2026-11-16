// 本地契约回归：先复用现有 TS 加载钩子和鉴权检查，不代表真实后端联调。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
import http from 'node:http'
const { request } = await import('../src/api/request.ts')
const { setSessionToken } = await import('../src/api/session.ts')
const { useNotificationsStore } = await import('../src/stores/notifications.ts')
const store = useNotificationsStore()
const { notificationTarget } = await import('../src/utils/notification-target.ts')
const { notificationError } = await import('../src/api/notifications.ts')
const axios = (await import('axios')).default
let rows = [1, 2, 3].map((id) => ({
  id,
  title: '通知',
  content: '内容',
  read: false,
  eventType: 'APPLICATION_ACCEPTED',
  targetType: 'APPLICATION',
  applicationId: id,
  recruitmentId: null,
  leaderTransferId: null,
  createdAt: '2026-11-13T10:00:00',
}))
let failRead = false,
  failList = false,
  writes = 0,
  releaseWrite,
  writeArrived
const server = http.createServer(async (req, res) => {
  res.setHeader('Content-Type', 'application/json')
  const url = new URL(req.url, 'http://localhost')
  assert.match(req.headers.authorization || '', /^Bearer /)
  if (req.method === 'PATCH') {
    writes++
    if (writeArrived) {
      writeArrived()
      await new Promise((resolve) => {
        releaseWrite = resolve
      })
    }
    if (failRead) {
      res.statusCode = 500
      res.end('{}')
      return
    }
    const id = Number(url.pathname.split('/')[2])
    rows = rows.map((row) =>
      url.pathname.endsWith('read-all') || row.id === id ? { ...row, read: true } : row,
    )
    res.end(JSON.stringify({ code: 0, data: null }))
    return
  }
  if (failList) {
    res.statusCode = 503
    res.end('{}')
    return
  }
  const page = Number(url.searchParams.get('page')),
    pageSize = Number(url.searchParams.get('pageSize'))
  const unreadOnly = url.searchParams.get('unreadOnly') === 'true'
  const filtered = rows.filter((row) => !unreadOnly || !row.read)
  res.end(
    JSON.stringify({
      code: 0,
      data: {
        records: filtered.slice((page - 1) * pageSize, page * pageSize),
        page,
        pageSize,
        total: filtered.length,
        unreadCount: rows.filter((row) => !row.read).length,
      },
    }),
  )
})
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
try {
  request.defaults.baseURL = `http://127.0.0.1:${server.address().port}`
  setSessionToken('notification-a')
  await store.load({ page: 3, pageSize: 1, unreadOnly: true })
  assert.equal(store.items.length, 1)
  assert.equal(store.unreadCount, 3, '未读数是全局值，不是当前页长度')
  failRead = true
  assert.equal(await store.markRead(3), false)
  assert.equal(store.items[0].read, false)
  assert.equal(store.unreadCount, 3)
  assert.ok(store.actionError)
  failRead = false
  assert.equal(await store.markRead(3), true)
  assert.equal(store.page, 2, '未读列表末页清空后回到有效页')
  assert.equal(store.total, 2)
  assert.equal(store.unreadCount, 2)
  await store.markAllRead()
  assert.equal(store.items.length, 0)
  assert.equal(store.page, 1)
  assert.equal(store.unreadCount, 0)
  failList = true
  await store.refreshUnreadCount()
  assert.equal(store.unreadCount, null, '请求失败不能当成未读为零')
  assert.ok(store.countError)
  assert.equal(await store.markRead(1), true)
  assert.match(store.notice, /刷新失败/)
  failList = false
  const arrived = new Promise((resolve) => {
    writeArrived = resolve
  })
  const action = store.markRead(1)
  await arrived
  const before = writes
  assert.equal(await store.markRead(2), false, '重复点击不发送第二次写请求')
  assert.equal(writes, before)
  setSessionToken('notification-b')
  assert.deepEqual(store.items, [])
  assert.equal(store.unreadCount, null)
  releaseWrite()
  assert.equal(await action, false)
  assert.equal(store.notice, '', '旧账号写请求不能修改新会话反馈')
  writeArrived = undefined
  await store.load({ page: 1, pageSize: 20, unreadOnly: false })
  assert.equal(store.items.length, 3)
  setSessionToken('')
  assert.deepEqual(store.items, [])
  assert.equal(store.unreadCount, null)
  const item = rows[0]
  assert.equal(notificationTarget(item, []).path, null)
  assert.deepEqual(notificationTarget(item, ['/my-applications']).query, { applicationId: '1' })
  assert.equal(
    notificationTarget({ ...item, eventType: 'APPLICATION_SUBMITTED' }, ['/my-recruitments']).path,
    '/my-recruitments',
  )
  assert.equal(
    notificationTarget({ ...item, targetType: 'RECRUITMENT', recruitmentId: 9 }, [
      '/recruitments/:id',
    ]).path,
    '/recruitments/9',
  )
  assert.equal(notificationTarget({ ...item, applicationId: -1 }, ['/my-applications']).path, null)
  assert.equal(
    notificationTarget({ ...item, eventType: 'FUTURE_EVENT' }, ['/my-applications']).path,
    null,
  )
  for (const status of [400, 401, 403, 404, 409, 500]) {
    const cause = new axios.AxiosError('error', undefined, undefined, undefined, { status })
    assert.ok(notificationError(cause).length > 0)
  }
  console.log(
    'PASS: 通知分页、全局未读数、读失败保留、末页回退、全部已读、刷新失败、重复提交与跨账号隔离',
  )
} finally {
  await new Promise((resolve) => server.close(resolve))
}
