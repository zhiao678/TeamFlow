// 仅指向独立测试库：创建负责人、成员、申请人账号与测试队伍。
import assert from 'node:assert/strict'
import { mkdir } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright')
const base = process.env.API_BASE || 'http://127.0.0.1:18080/api'
const origin = process.env.FRONTEND_URL || 'http://127.0.0.1:5173'
const output = process.env.OUTPUT_DIR || join(tmpdir(), 'teamflow-member-side-e2e')
await mkdir(output, { recursive: true })
async function call(path, method = 'GET', body, token, expected = 0) {
  const response = await fetch(base + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(method === 'GET' || body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  const result = await response.json()
  assert.equal(result.code, expected, `${path}: ${JSON.stringify(result)}`)
  return result.data
}
const suffix = Date.now().toString().slice(-9),
  password = 'Test-pass-2026'
async function register(prefix, name) {
  return call('/auth/register', 'POST', { username: prefix + suffix, password, displayName: name })
}
const owner = await register('leader', '验收负责人'),
  member = await register('member', '验收成员'),
  applicant = await register('visitor', '验收申请人')
async function makeTeam(label, addApplicant = false) {
  const item = await call(
    '/recruitments',
    'POST',
    {
      categoryId: 1,
      title: label + ' ' + suffix,
      purpose: '课程实践',
      maxMembers: 4,
      tagIds: [1, 2],
      publish: true,
    },
    owner.token,
  )
  const application = await call(`/recruitments/${item.id}/applications`, 'POST', {}, member.token)
  await call(`/applications/${application.id}/accept`, 'PATCH', {}, owner.token)
  if (addApplicant) {
    const extra = await call(`/recruitments/${item.id}/applications`, 'POST', {}, applicant.token)
    await call(`/applications/${extra.id}/accept`, 'PATCH', {}, owner.token)
  }
  await call(`/recruitments/${item.id}/form-team`, 'POST', {}, owner.token)
  return item
}
const team = await makeTeam('退出流程队伍')
const browser = await chromium.launch({
  headless: true,
  executablePath: process.env.CHROMIUM_EXECUTABLE,
})
const errors = [],
  pages = []
async function userPage(token) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
  if (token)
    await context.addInitScript((value) => localStorage.setItem('teamflow.token', value), token)
  const page = await context.newPage()
  pages.push(page)
  page.on('pageerror', (error) => errors.push(error.message))
  return page
}
const page = await userPage(),
  ownerPage = await userPage(owner.token),
  applicantPage = await userPage(applicant.token)
try {
  await page.goto(origin + '/my-teams')
  await page.waitForURL((url) => url.pathname === '/login')
  await page.getByLabel('用户名', { exact: true }).fill('member' + suffix)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((url) => url.pathname === '/my-teams')
  await page.getByRole('heading', { name: team.title, exact: true }).waitFor()
  await page.getByRole('button', { name: '进入队伍群', exact: true }).click()
  const group = await call(
    `/recruitments/${team.id}/team-conversation`,
    'GET',
    undefined,
    member.token,
  )
  await page.waitForURL((url) => url.pathname === `/conversations/${group.id}`)
  await page.goto(origin + `/recruitments/${team.id}`)
  await page.getByRole('button', { name: '联系负责人', exact: true }).click()
  await page.waitForURL((url) => /^\/conversations\/\d+$/.test(url.pathname))
  const directPath = new URL(page.url()).pathname
  assert.equal(new URL(page.url()).searchParams.get('sourceRecruitmentId'), String(team.id))
  await page.goto(origin + `/recruitments/${team.id}`)
  await page.getByRole('button', { name: '联系负责人', exact: true }).click()
  await page.waitForURL((url) => url.pathname === directPath)
  await ownerPage.goto(origin + `/recruitments/${team.id}`)
  await ownerPage.getByText('负责人发起转交入口待管理模块接入。', { exact: true }).waitFor()
  assert.equal(await ownerPage.getByRole('button', { name: '联系负责人', exact: true }).count(), 0)
  assert.equal(
    await ownerPage.getByRole('button', { name: '申请退出队伍', exact: true }).count(),
    0,
  )
  await applicantPage.goto(origin + `/recruitments/${team.id}`)
  await applicantPage.getByText('你当前没有该队伍群的访问资格。', { exact: true }).waitFor()
  assert.equal(
    await applicantPage.getByRole('button', { name: '申请退出队伍', exact: true }).count(),
    0,
  )
  await page.goto(origin + `/recruitments/${team.id}`)
  await page.getByLabel('退出原因（选填）').fill('课程时间冲突')
  await page.getByRole('button', { name: '申请退出队伍', exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  assert.equal((await call('/member-exit-requests/me', 'GET', undefined, member.token)).length, 0)
  await page.getByRole('button', { name: '申请退出队伍', exact: true }).click()
  await page.getByRole('button', { name: '确认提交退出申请', exact: true }).click()
  await page
    .getByText('等待负责人审批。审批通过前仍保留成员资格、人数及群聊入口。', { exact: true })
    .waitFor()
  assert.equal(
    (await call(`/recruitments/${team.id}/members`, 'GET', undefined, member.token)).length,
    2,
  )
  await page.getByRole('button', { name: '进入队伍群', exact: true }).click()
  await page.waitForURL((url) => url.pathname === `/conversations/${group.id}`)
  let exits = await call('/member-exit-requests/me', 'GET', undefined, member.token)
  await call(
    `/member-exit-requests/${exits[0].id}/reject`,
    'PATCH',
    { reason: '请先完成交接' },
    owner.token,
  )
  await page.goto(origin + `/recruitments/${team.id}`)
  await page.getByRole('button', { name: '申请退出队伍', exact: true }).waitFor()
  assert.ok((await page.locator('body').innerText()).includes('请先完成交接'))
  await page.getByRole('button', { name: '申请退出队伍', exact: true }).click()
  await page.getByRole('button', { name: '确认提交退出申请', exact: true }).click()
  await page
    .getByText('等待负责人审批。审批通过前仍保留成员资格、人数及群聊入口。', { exact: true })
    .waitFor()
  exits = await call('/member-exit-requests/me', 'GET', undefined, member.token)
  await call(`/member-exit-requests/${exits[0].id}/approve`, 'PATCH', {}, owner.token)
  await page.goto(origin + '/my-member-exits')
  await page.getByText('已批准', { exact: true }).waitFor()
  await page.goto(origin + '/my-teams')
  await page.getByRole('heading', { name: '暂无可访问的已成队队伍', exact: true }).waitFor()
  await call(`/recruitments/${team.id}/team-conversation`, 'GET', undefined, member.token, 40300)
  console.log(
    'PASS: real exit request/reject/reapply/approve, private reuse, guest and three-role permissions',
  )
  const transferTeam = await makeTeam('转交流程队伍', true)
  let transfer = await call(
    `/recruitments/${transferTeam.id}/leader-exit-requests`,
    'POST',
    { successorId: member.user.id },
    owner.token,
  )
  await ownerPage.goto(origin + '/my-leader-transfers')
  await ownerPage.getByText('待确认', { exact: true }).waitFor()
  assert.equal(await ownerPage.getByRole('button', { name: '接受转交', exact: true }).count(), 0)
  await page.goto(origin + '/my-leader-transfers?transferId=' + transfer.id)
  await page.locator('.application-highlight').waitFor()
  await page.getByRole('button', { name: '拒绝转交', exact: true }).click()
  await page.getByRole('button', { name: '确认拒绝', exact: true }).click()
  await page.getByText('已拒绝', { exact: true }).waitFor()
  assert.equal(
    (await call(`/recruitments/${transferTeam.id}`, 'GET', undefined, member.token)).ownerId,
    owner.user.id,
  )
  transfer = await call(
    `/recruitments/${transferTeam.id}/leader-exit-requests`,
    'POST',
    { successorId: member.user.id },
    owner.token,
  )
  await page.goto(origin + '/my-leader-transfers?transferId=' + transfer.id)
  await page.getByRole('button', { name: '接受转交', exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  assert.equal(
    (await call(`/recruitments/${transferTeam.id}`, 'GET', undefined, member.token)).ownerId,
    owner.user.id,
  )
  await page.getByRole('button', { name: '接受转交', exact: true }).click()
  await page.getByRole('button', { name: '确认接受', exact: true }).click()
  await page.getByText('已接受', { exact: true }).waitFor()
  await page.getByText('2 / 4 人 · 我的身份：负责人', { exact: true }).waitFor()
  assert.equal(
    (await call(`/recruitments/${transferTeam.id}`, 'GET', undefined, member.token)).ownerId,
    member.user.id,
  )
  await call(
    `/recruitments/${transferTeam.id}/team-conversation`,
    'GET',
    undefined,
    owner.token,
    40300,
  )
  await page.screenshot({ path: join(output, 'transfer.png'), fullPage: true })
  await page.goto(origin + '/my-leader-transfers?transferId=999999')
  await page
    .getByText('对应转交记录不可用，以下仍展示你的全部转交记录。', { exact: true })
    .waitFor()
  // 陈旧页面遇到真实 409，不提示成功，不自动重试写入。
  const conflictTeam = await makeTeam('冲突验证队伍', true)
  const conflict = await call(
    `/recruitments/${conflictTeam.id}/leader-exit-requests`,
    'POST',
    { successorId: member.user.id },
    owner.token,
  )
  await page.goto(origin + '/my-leader-transfers?transferId=' + conflict.id)
  await page.getByRole('button', { name: '接受转交', exact: true }).click()
  await call(`/leader-transfers/${conflict.id}/reject`, 'PATCH', {}, member.token)
  await page.getByRole('button', { name: '确认接受', exact: true }).click()
  await page
    .getByText('状态已变化或重复操作，请重新加载实际结果，不要自动重复提交。', { exact: true })
    .waitFor()
  await page.locator(`#transfer-${conflict.id}`).getByText('已拒绝', { exact: true }).waitFor()
  console.log(
    'PASS: real transfer reject/accept, successor-only actions, owner exit, targeting and stale-write 409',
  )
  // 写入成功但后续列表失败：保留成功结果，提供刷新而不是重复提交。
  const refreshTransfer = await call(
    `/recruitments/${conflictTeam.id}/leader-exit-requests`,
    'POST',
    { successorId: member.user.id },
    owner.token,
  )
  await page.goto(origin + '/my-leader-transfers?transferId=' + refreshTransfer.id)
  await page.getByRole('button', { name: '接受转交', exact: true }).click()
  await page.route('**/api/leader-transfers/me', (r) =>
    r.fulfill({ status: 500, json: { code: 50000, message: 'refresh failure' } }),
  )
  await page.getByRole('button', { name: '确认接受', exact: true }).click()
  await page.getByRole('button', { name: '重试转交记录', exact: true }).waitFor()
  assert.ok((await page.locator('body').innerText()).includes('操作已完成，但记录刷新失败'))
  await page.unroute('**/api/leader-transfers/me')
  await page.getByRole('button', { name: '重试转交记录', exact: true }).click()
  await page
    .locator(`#transfer-${refreshTransfer.id}`)
    .getByText('已接受', { exact: true })
    .waitFor()
  // 服务端只读会话仍保留在我的队伍中；受控响应只用于异常/兼容性验收。
  await page.route('**/api/conversations', async (route) => {
    const response = await route.fetch()
    const json = await response.json()
    json.data = json.data.map((row) =>
      row.recruitmentId === transferTeam.id ? { ...row, status: 'READ_ONLY' } : row,
    )
    await route.fulfill({ response, json })
  })
  await page.goto(origin + '/my-teams')
  await page.getByRole('button', { name: '查看群聊记录', exact: true }).waitFor()
  await page.unroute('**/api/conversations')
  console.log('PASS: mutation success with refresh failure, read-only conversation retained')
  // 单条失败与列表失败分别显示；恢复后重试。
  await page.route(`**/api/recruitments/${conflictTeam.id}/members`, (r) =>
    r.fulfill({ status: 500, json: { code: 50000, message: 'test' } }),
  )
  await page.goto(origin + '/my-teams')
  await page.getByRole('heading', { name: '队伍信息暂不可用', exact: true }).waitFor()
  await page.getByRole('heading', { name: transferTeam.title, exact: true }).waitFor()
  await page.unroute(`**/api/recruitments/${conflictTeam.id}/members`)
  await page.getByRole('button', { name: '重试队伍信息', exact: true }).click()
  await page.getByRole('heading', { name: conflictTeam.title, exact: true }).waitFor()
  await page.route('**/api/conversations', (r) =>
    r.fulfill({ status: 500, json: { code: 50000, message: 'test' } }),
  )
  await page.getByRole('button', { name: '刷新队伍', exact: true }).click()
  await page.getByRole('button', { name: '重试队伍列表', exact: true }).waitFor()
  assert.equal(
    await page.getByRole('heading', { name: '暂无可访问的已成队队伍', exact: true }).count(),
    0,
  )
  await page.unroute('**/api/conversations')
  await page.getByRole('button', { name: '重试队伍列表', exact: true }).click()
  await page.getByRole('heading', { name: transferTeam.title, exact: true }).waitFor()
  await page.screenshot({ path: join(output, 'teams.png'), fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: join(output, 'teams-mobile.png'), fullPage: true })
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
  assert.deepEqual(errors, [])
  console.log('PASS: team partial/list errors and retry, mobile layout, no page exceptions')
} catch (error) {
  for (let i = 0; i < pages.length; i++) {
    console.log('PAGE', i, pages[i].url(), await pages[i].locator('body').innerText())
    await pages[i].screenshot({ path: join(output, `failure-${i}.png`), fullPage: true })
  }
  throw error
} finally {
  await browser.close()
}
