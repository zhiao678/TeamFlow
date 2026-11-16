// 在独立测试库上运行：会注册两个账号并创建招募。
import { mkdir } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE || 'playwright')
const outputDir = process.env.OUTPUT_DIR || join(tmpdir(), 'teamflow-applicant-e2e')
await mkdir(outputDir, { recursive: true })
import assert from 'node:assert/strict'
const base = process.env.API_BASE || 'http://127.0.0.1:18080/api'
async function api(path, method = 'GET', body, token) {
  const response = await fetch(base + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(method === 'GET' || body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  const result = await response.json()
  assert.equal(result.code, 0, `${path}: ${JSON.stringify(result)}`)
  return result.data
}
const suffix = Date.now().toString().slice(-9),
  password = 'Test-pass-2026'
const owner = await api('/auth/register', 'POST', {
  username: 'owner' + suffix,
  password,
  displayName: '测试负责人',
})
const applicant = await api('/auth/register', 'POST', {
  username: 'applicant' + suffix,
  password,
  displayName: '测试申请人',
})
const recruitment = await api(
  '/recruitments',
  'POST',
  {
    categoryId: 1,
    title: '招募发现验收 ' + suffix,
    purpose: '完成课程项目',
    description: '共同完成 TeamFlow 课程实践',
    maxMembers: 3,
    tagIds: [1, 2],
    publish: true,
  },
  owner.token,
)
const browser = await chromium.launch({
  headless: true,
  executablePath: process.env.CHROMIUM_EXECUTABLE,
})
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } }),
  page = await context.newPage()
const errors = []
page.on('pageerror', (error) => errors.push(error.message))
const origin = process.env.FRONTEND_URL || 'http://127.0.0.1:5173'
try {
  await page.goto(origin + '/recruitments?tagIds=1,2&minRemainingSlots=0&sort=POPULAR')
  await page.getByRole('link', { name: recruitment.title, exact: true }).waitFor()
  assert.equal(await page.getByLabel('最少剩余名额').inputValue(), '0')
  await page
    .getByRole('article')
    .filter({ hasText: recruitment.title })
    .getByRole('button', { name: '☆ 收藏', exact: true })
    .click()
  await page.waitForURL('**/login?**')
  assert.ok(new URL(page.url()).searchParams.get('redirect').includes('minRemainingSlots=0'))
  await page.getByLabel('用户名', { exact: true }).fill('applicant' + suffix)
  await page.getByLabel('密码', { exact: true }).fill(password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForURL((url) => url.pathname === '/recruitments')
  const card = page.getByRole('article').filter({ hasText: recruitment.title })
  await card.getByRole('button', { name: '☆ 收藏', exact: true }).waitFor()
  await card.getByRole('button', { name: '☆ 收藏', exact: true }).click()
  await card.getByRole('button', { name: '★ 取消收藏', exact: true }).waitFor()
  await page.getByRole('link', { name: recruitment.title, exact: true }).click()
  await page.getByRole('button', { name: '★ 取消收藏', exact: true }).waitFor()
  await page.getByRole('button', { name: '提交申请', exact: true }).waitFor()
  await page.getByLabel('自我介绍').fill('希望参与前端开发')
  await page.getByLabel('技能说明').fill('Vue / TypeScript')
  await page.getByLabel('可投入时间').fill('每周六小时')
  await page.getByRole('button', { name: '提交申请', exact: true }).click()
  await page.getByRole('button', { name: '撤回申请', exact: true }).waitFor()
  assert.equal(await page.getByRole('button', { name: '提交申请', exact: true }).count(), 0)
  await page.getByRole('button', { name: '撤回申请', exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  assert.equal(
    (await api('/applications/me', 'GET', undefined, applicant.token))[0].status,
    'PENDING',
  )
  await page.getByRole('button', { name: '撤回申请', exact: true }).click()
  await page.getByRole('button', { name: '确认撤回', exact: true }).click()
  await page.getByRole('button', { name: '提交申请', exact: true }).waitFor()
  await page.getByRole('button', { name: '提交申请', exact: true }).click()
  await page.getByRole('button', { name: '撤回申请', exact: true }).waitFor()
  let rows = await api('/applications/me', 'GET', undefined, applicant.token)
  assert.equal(rows.length, 2)
  await api(
    `/applications/${rows[0].id}/reject`,
    'PATCH',
    { reason: '时间安排不合适' },
    owner.token,
  )
  await page.reload()
  await page.getByRole('button', { name: '提交申请', exact: true }).waitFor()
  assert.ok((await page.locator('body').innerText()).includes('时间安排不合适'))
  await page.getByRole('button', { name: '提交申请', exact: true }).click()
  await page.getByRole('button', { name: '撤回申请', exact: true }).waitFor()
  rows = await api('/applications/me', 'GET', undefined, applicant.token)
  await api(`/applications/${rows[0].id}/accept`, 'PATCH', {}, owner.token)
  await page.reload()
  await page.getByText('你已是该队伍的正式成员。', { exact: true }).waitFor()
  assert.equal(await page.getByRole('button', { name: '提交申请', exact: true }).count(), 0)
  await page.screenshot({ path: join(outputDir, 'detail.png'), fullPage: true })
  await page.goto(origin + '/my-applications?applicationId=' + rows[0].id)
  await page.locator('.application-highlight').waitFor()
  assert.equal(await page.locator('article').count(), 3)
  await page.goto(origin + '/my-applications?applicationId=999999')
  await page
    .getByText('未找到通知对应的申请，以下仍展示你的全部申请记录。', { exact: true })
    .waitFor()
  await page.goto(origin + '/my-favorites')
  await page.getByRole('button', { name: '★ 取消收藏', exact: true }).click()
  await page.getByRole('heading', { name: '还没有收藏' }).waitFor()
  // 查询失败必须禁用申请，并支持重试。
  await page.route('**/api/recruitments/*/members', (route) =>
    route.fulfill({ status: 500, json: { code: 50000, message: 'test failure' } }),
  )
  await page.goto(origin + '/recruitments/' + recruitment.id)
  await page.getByText('成员加载失败，请检查网络或稍后重试。', { exact: false }).waitFor()
  assert.equal(await page.getByRole('button', { name: '提交申请', exact: true }).count(), 0)
  await page.unroute('**/api/recruitments/*/members')
  await page.getByRole('button', { name: '重试', exact: true }).click()
  await page.getByText('你已是该队伍的正式成员。', { exact: true }).waitFor()
  await page.setViewportSize({ width: 390, height: 844 })
  await page.screenshot({ path: join(outputDir, 'mobile.png'), fullPage: true })
  assert.ok(
    await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
    '移动端无横向溢出',
  )
  assert.deepEqual(errors, [])
  console.log(
    'PASS: real Spring Boot/H2 two-account UI flow: login return, filters, favorites, submit, cancel withdrawal, withdraw, reapply, reject, reapply, accept/member gate, history, notification target, member error/retry, mobile layout',
  )
} catch (error) {
  console.log('URL', page.url())
  console.log(await page.locator('body').innerText())
  console.log('ERRORS', errors)
  await page.screenshot({ path: join(outputDir, 'failure.png'), fullPage: true })
  throw error
} finally {
  await browser.close()
}
