// 复用 TS 加载钩子并先执行已有鉴权回归；随后验证新版接口契约。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
import http from 'node:http'
const { request } = await import('../src/api/request.ts')
const { setSessionToken } = await import('../src/api/session.ts')
const { getRecruitments } = await import('../src/api/recruitment-query.ts')
const management = await import('../src/api/recruitment-management.ts')
const { listTags, listCategories } = await import('../src/api/dictionaries.ts')
const { useAuthStore } = await import('../src/stores/auth.ts')
const { updateProfile } = await import('../src/api/users.ts')

let last
let fail = false
const backend = http.createServer(async (req, res) => {
  let body = ''
  for await (const chunk of req) body += chunk
  last = {
    url: new URL(req.url, 'http://localhost'),
    method: req.method,
    body: body ? JSON.parse(body) : null,
    token: req.headers.authorization,
  }
  res.setHeader('Content-Type', 'application/json')
  if (fail) {
    res.statusCode = 503
    res.end('{}')
    return
  }
  let data = null
  if (last.url.pathname === '/recruitments')
    data = { records: [{ id: 1 }], page: 2, pageSize: 20, total: 41 }
  if (last.url.pathname === '/recruitments/mine') data = [{ id: 7, status: 'DRAFT' }]
  if (['/tags', '/categories'].includes(last.url.pathname)) data = [{ id: 1, name: 'Vue' }]
  if (last.url.pathname === '/users/me') data = { id: 1, skills: [], ...last.body }
  res.end(JSON.stringify({ code: 0, message: 'success', data }))
})
await new Promise((resolve) => backend.listen(0, '127.0.0.1', resolve))
try {
  request.defaults.baseURL = `http://127.0.0.1:${backend.address().port}`
  setSessionToken('adaptation-token')
  const params = {
    keyword: 'Vue',
    categoryId: 1,
    courseName: '课程',
    teacherName: '教师',
    status: 'RECRUITING',
    page: 2,
    pageSize: 20,
    sort: 'DEADLINE',
  }
  const page = await getRecruitments(params)
  assert.equal(page.total, 41)
  assert.equal(page.page, 2)
  for (const [key, value] of Object.entries(params))
    assert.equal(last.url.searchParams.get(key), String(value))
  assert.deepEqual(await listTags(), [{ id: 1, name: 'Vue' }])
  assert.equal((await listCategories()).length, 1)
  assert.equal((await management.listMyRecruitments())[0].id, 7)
  const input = {
    categoryId: 1,
    title: '标题',
    courseName: '课程',
    teacherName: '教师',
    purpose: '目的',
    description: '说明',
    requirements: '要求',
    maxMembers: 4,
    deadlineAt: '2026-12-01T18:00:00',
    tagIds: [1],
  }
  await management.updateRecruitment(7, input, { status: 'RECRUITING', includeTagIds: true })
  assert.deepEqual(
    Object.keys(last.body).sort(),
    ['description', 'requirements', 'tagIds', 'deadlineAt', 'maxMembers'].sort(),
  )
  await management.updateRecruitment(7, input, { status: 'DRAFT', includeTagIds: true })
  assert.equal(last.body.title, input.title)
  await assert.rejects(async () =>
    management.updateRecruitment(7, input, { status: 'CLOSED', includeTagIds: true }),
  )
  await management.deleteRecruitmentDraft(7)
  assert.equal(last.method, 'DELETE')
  assert.equal(last.url.pathname, '/recruitments/7')
  await updateProfile({ displayName: '用户', tagIds: [] })
  assert.deepEqual(last.body.tagIds, [])
  const auth = useAuthStore()
  await auth.logout()
  assert.equal(last.url.pathname, '/auth/logout')
  assert.equal(last.token, 'Bearer adaptation-token')
  assert.equal(auth.token, '')
  setSessionToken('second-token')
  fail = true
  await assert.rejects(listTags)
  await assert.rejects(management.listMyRecruitments)
  await auth.logout()
  assert.equal(auth.token, '', '服务端退出失败也清除本地会话')
  console.log('PASS: 服务端分页参数、字典、我的招募、编辑白名单、删除、技能清空、退出及失败处理')
} finally {
  await new Promise((resolve) => backend.close(resolve))
}
