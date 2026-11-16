// 独立契约回归；业务界面始终调用真实 API，不包含模拟成功回退。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
const { request } = await import('../src/api/request.ts')
const { setSessionToken } = await import('../src/api/session.ts')
const { useFavoritesStore } = await import('../src/stores/favorites.ts')
const { useApplicationsStore } = await import('../src/stores/applications.ts')
const { discoveryQuery } = await import('../src/utils/recruitment-discovery.ts')
const { latestApplication, applicationBlock } =
  await import('../src/utils/application-eligibility.ts')
const { getRecruitments } = await import('../src/api/recruitment-query.ts')
const { listFavorites } = await import('../src/api/favorites.ts')
const favorites = useFavoritesStore(),
  applications = useApplicationsStore()
const row = {
  id: 12,
  favorited: false,
  ownerId: 1,
  status: 'RECRUITING',
  remainingSlots: 1,
  deadlineAt: null,
}
const history = [
  { id: 1, recruitmentId: 12, createdAt: '2026-11-16T10:00:00', status: 'ACCEPTED' },
  { id: 2, recruitmentId: 12, createdAt: '2026-11-16T10:00:00', status: 'WITHDRAWN' },
]
let requests = [],
  failWrite = false,
  failRead = false,
  holdWrite
request.defaults.adapter = async (config) => {
  requests.push(config)
  if (config.method === 'get') {
    if (failRead && config.url === '/applications/me')
      return { data: { code: 50000, message: 'failure' }, status: 200, config }
    const data =
      config.url === '/applications/me'
        ? history
        : config.url === '/notifications'
          ? { records: [], unreadCount: 0 }
          : config.url === '/recruitments/favorites'
            ? [row]
            : { records: [row], total: 1, page: 1, pageSize: 20 }
    return { data: { code: 0, data }, status: 200, config }
  }
  if (holdWrite) await holdWrite
  return {
    data: failWrite
      ? { code: 40900, message: 'a pending application already exists' }
      : { code: 0, data: { ...history[1], status: 'PENDING' } },
    status: 200,
    config,
  }
}
setSessionToken('discovery-a')
const query = discoveryQuery({
  tagIds: '2,1,2,bad,-1',
  minRemainingSlots: '0',
  sort: 'POPULAR',
  page: '3',
  pageSize: '500',
  status: 'ALL',
})
assert.equal(query.minRemainingSlots, 0)
assert.equal(query.tagIds, '2,1')
assert.equal(query.sort, 'POPULAR')
assert.equal(query.status, 'RECRUITING')
assert.equal(query.page, 3)
assert.equal(query.pageSize, 100)
assert.equal(discoveryQuery({ minRemainingSlots: '-1' }).minRemainingSlots, undefined)
assert.equal(discoveryQuery({ minRemainingSlots: '1.5' }).minRemainingSlots, undefined)
await getRecruitments(query)
assert.equal(requests.at(-1).params.tagIds, '2,1')
assert.equal(requests.at(-1).params.minRemainingSlots, 0)
assert.ok(Array.isArray(await listFavorites()))
assert.equal(latestApplication(history, 12).id, 2)
assert.equal(
  applicationBlock(row, 2, [], history[0], Date.now()),
  '',
  '历史接受但不在正式成员名单，允许再次申请',
)
assert.match(applicationBlock(row, 2, [2], undefined, Date.now()), /正式成员/)
assert.match(applicationBlock(row, 1, [], undefined, Date.now()), /负责人/)
assert.match(applicationBlock({ ...row, remainingSlots: 0 }, 2, [], undefined, Date.now()), /满员/)
assert.match(
  applicationBlock({ ...row, deadlineAt: '2000-01-01T00:00:00' }, 2, [], undefined, Date.now()),
  /截止/,
)
assert.match(applicationBlock(row, 2, [], { status: 'PENDING' }, Date.now()), /待处理/)
const ingestOld = favorites.reader()
favorites.reader()([row])
await favorites.toggle(12)
ingestOld([row])
assert.equal(favorites.states[12], true, '迟到的读取不能覆盖收藏写入')
failWrite = true
assert.equal(await favorites.toggle(12), false)
assert.equal(favorites.states[12], true)
assert.ok(favorites.errors[12])
failWrite = false
let release
holdWrite = new Promise((resolve) => {
  release = resolve
})
const pending = favorites.toggle(12)
assert.equal(await favorites.toggle(12), false, '同一招募禁止重复写入')
setSessionToken('discovery-b')
release()
await pending
holdWrite = undefined
assert.deepEqual(favorites.states, {}, '账号切换后忽略旧写入')
ingestOld([row])
assert.deepEqual(favorites.states, {}, '账号切换后忽略旧读取')
await applications.load()
assert.equal(applications.items.length, 2)
failRead = true
await applications.load()
assert.equal(applications.ready, false)
assert.ok(applications.error)
failRead = false
await applications.load()
assert.equal(
  await applications.submit(12, { introduction: '', skillsDescription: '', availableTime: '' }),
  true,
)
const submission = requests.findLast((config) => config.url === '/recruitments/12/applications')
assert.deepEqual(JSON.parse(submission.data), {
  introduction: '',
  skillsDescription: '',
  availableTime: '',
})
assert.ok(requests.some((config) => config.url === '/notifications'))
failWrite = true
assert.equal(await applications.withdraw(2), true, '冲突要求调用方刷新招募与成员')
assert.match(applications.actionError, /待处理申请/)
failWrite = false
failRead = true
await applications.withdraw(2)
assert.match(applications.notice, /已撤回.*刷新失败/)
assert.equal(applications.ready, false)
failRead = false
holdWrite = new Promise((resolve) => {
  release = resolve
})
const oldApplication = applications.submit(12, {})
assert.equal(await applications.submit(12, {}), false)
setSessionToken('discovery-c')
release()
await oldApplication
assert.deepEqual(applications.items, [])
assert.equal(applications.notice, '')
console.log(
  'PASS: discovery filters, eligibility, favorites races/account isolation, application lifecycle and failure contracts',
)
