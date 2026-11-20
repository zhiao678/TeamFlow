// 复用既有 TS 加载钩子；使用本地契约服务，不代表真实后端或页面联调通过。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
import http from 'node:http'
const { request } = await import('../src/api/request.ts')
const { setSessionToken } = await import('../src/api/session.ts')
const chat = await import('../src/api/conversations.ts')
const team = await import('../src/api/team-management.ts')
const { teamErrorStatus } = await import('../src/api/team-result.ts')
const { teamRoutes, createTeamRoutes, teamRoutePath } = await import('../src/router/team-routes.ts')
const { createMessageDraft, mergeConversationMessages } =
  await import('../src/utils/conversation-messages.ts')
const { useTeamSyncStore } = await import('../src/stores/team-sync.ts')
let last,
  result = {},
  status = 200,
  code = 0,
  calls = 0
const server = http.createServer(async (req, res) => {
  let text = ''
  for await (const chunk of req) text += chunk
  calls++
  last = {
    method: req.method,
    url: new URL(req.url, 'http://localhost'),
    body: text ? JSON.parse(text) : undefined,
    token: req.headers.authorization,
  }
  res.statusCode = status
  res.setHeader('Content-Type', 'application/json')
  res.end(JSON.stringify({ code, message: 'contract', data: result }))
})
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve))
try {
  request.defaults.baseURL = `http://127.0.0.1:${server.address().port}`
  setSessionToken('foundation-a')
  async function check(call, method, path, body) {
    await call()
    assert.equal(last.method, method)
    assert.equal(last.url.pathname, path)
    assert.equal(last.token, 'Bearer foundation-a')
    assert.deepEqual(last.body, body)
  }
  await check(() => chat.listConversations(), 'GET', '/conversations')
  await check(() => chat.createPrivateConversation(9), 'POST', '/conversations/private', {
    targetUserId: 9,
  })
  await check(() => chat.getTeamConversation(3), 'GET', '/recruitments/3/team-conversation')
  await check(
    () => chat.listMessages(12, { page: 2, pageSize: 50, afterMessageId: 20 }),
    'GET',
    '/conversations/12/messages',
  )
  assert.equal(last.url.searchParams.get('afterMessageId'), '20')
  assert.equal(last.url.searchParams.get('page'), '2')
  const draft = createMessageDraft('你好', 3)
  assert.ok(draft.clientMessageId.length <= 64)
  await check(() => chat.sendMessage(12, draft), 'POST', '/conversations/12/messages', draft)
  await check(() => chat.sendMessage(12, draft), 'POST', '/conversations/12/messages', draft)
  await check(() => chat.markConversationRead(12, 20), 'PATCH', '/conversations/12/read', {
    lastReadMessageId: 20,
  })
  await check(() => team.formTeam(3), 'POST', '/recruitments/3/form-team')
  await check(() => team.removeTeamMember(3, 9), 'DELETE', '/recruitments/3/members/9')
  await check(() => team.requestMemberExit(3, '原因'), 'POST', '/recruitments/3/exit-requests', {
    reason: '原因',
  })
  await check(() => team.listRecruitmentExits(3), 'GET', '/recruitments/3/exit-requests')
  await check(() => team.listMyMemberExits(), 'GET', '/member-exit-requests/me')
  await check(() => team.approveMemberExit(4), 'PATCH', '/member-exit-requests/4/approve')
  await check(() => team.rejectMemberExit(4, '原因'), 'PATCH', '/member-exit-requests/4/reject', {
    reason: '原因',
  })
  await check(
    () => team.requestLeadershipExit(3, 9),
    'POST',
    '/recruitments/3/leader-exit-requests',
    { successorId: 9 },
  )
  await check(() => team.listMyLeaderTransfers(), 'GET', '/leader-transfers/me')
  await check(() => team.acceptLeaderTransfer(8), 'PATCH', '/leader-transfers/8/accept')
  await check(() => team.rejectLeaderTransfer(8), 'PATCH', '/leader-transfers/8/reject')
  code = 40900
  await assert.rejects(team.formTeam(3), (error) => teamErrorStatus(error) === 409)
  code = 0
  status = 403
  await assert.rejects(chat.listMessages(12), (error) => teamErrorStatus(error) === 403)
  status = 200
  const canceled = new AbortController()
  canceled.abort()
  const before = calls
  await assert.rejects(chat.listMessages(12, {}, canceled.signal))
  assert.equal(calls, before)
  assert.deepEqual(createTeamRoutes({}), [], '不注册占位路由')
  const routes = createTeamRoutes({ myTeams: async () => ({ default: {} }) })
  assert.equal(routes.length, 1)
  assert.equal(routes[0].name, teamRoutes.myTeams.name)
  assert.equal(routes[0].meta.requiresAuth, true)
  assert.equal(teamRoutePath('conversation', 12), '/conversations/12')
  assert.throws(() => teamRoutePath('conversation', -1))
  const message = { id: 2, conversationId: 12, content: '旧内容' }
  const merged = mergeConversationMessages(
    12,
    [message],
    [
      { ...message, content: '服务端内容' },
      { ...message, id: 1 },
    ],
  )
  assert.deepEqual(
    merged.map((item) => item.id),
    [1, 2],
  )
  assert.equal(merged[1].content, '服务端内容')
  assert.throws(() => mergeConversationMessages(13, [], [message]))
  assert.throws(() => createMessageDraft(' '))
  const sync = useTeamSyncStore()
  const session = sync.sessionRevision
  result = { records: [], page: 1, pageSize: 1, total: 0, unreadCount: 2 }
  assert.equal(await sync.afterTeamMutation(3, session), true)
  assert.equal(sync.revisions[3], 1)
  assert.equal(last.url.pathname, '/notifications')
  setSessionToken('foundation-b')
  const count = calls
  assert.equal(await sync.afterTeamMutation(3, session), false, '旧会话不得刷新新账号')
  assert.equal(calls, count)
  assert.deepEqual(sync.revisions, {})
  setSessionToken('')
  console.log(
    'PASS: 队伍/聊天接口契约、业务与权限错误、取消请求、路由注册、幂等消息草稿、合并去重、刷新与会话隔离',
  )
} finally {
  await new Promise((resolve) => server.close(resolve))
}
