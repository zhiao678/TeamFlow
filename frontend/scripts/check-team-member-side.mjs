// 页面逻辑契约回归：不替代真实三账号浏览器验收。
await import('./check-auth.mjs')
import assert from 'node:assert/strict'
import { effectScope, ref, nextTick } from 'vue'
const { request } = await import('../src/api/request.ts')
const { setSessionToken } = await import('../src/api/session.ts')
const { useAuthStore } = await import('../src/stores/auth.ts')
const { useTeamSyncStore } = await import('../src/stores/team-sync.ts')
const { useTeamRead } = await import('../src/composables/useTeamRead.ts')
const { useTeamAction } = await import('../src/composables/useTeamAction.ts')
const { useMyTeams } = await import('../src/composables/useMyTeams.ts')
const { mapTeams, canRespondToTransfer, transferEffect } =
  await import('../src/utils/team-member-side.ts')
const { teamErrorStatus, TeamRequestError } = await import('../src/api/team-result.ts')
const waitTick = () => new Promise((resolve) => setImmediate(resolve))
setSessionToken('member-a')
const auth = useAuthStore(),
  sync = useTeamSyncStore()
auth.user = { id: 7 }
const incoming = { status: 'PENDING', toUserId: 7, fromUserId: 2, exitAfterTransfer: true }
assert.equal(canRespondToTransfer(incoming, 7), true)
assert.equal(canRespondToTransfer(incoming, 2), false)
assert.equal(canRespondToTransfer({ ...incoming, status: 'EXPIRED' }, 7), false)
assert.match(transferEffect(incoming), /退出队伍/)
assert.match(transferEffect({ ...incoming, exitAfterTransfer: false }), /普通成员留队/)
const legacy = Object.assign(new Error('legacy'), { code: 40300 })
assert.equal(teamErrorStatus(legacy), 403)
let active = 0,
  peak = 0
const mapped = await mapTeams([1, 2, 3, 4, 5, 6, 7], new AbortController().signal, async (row) => {
  active++
  peak = Math.max(peak, active)
  await waitTick()
  active--
  return row * 2
})
assert.equal(peak, 3)
assert.deepEqual(mapped, [2, 4, 6, 8, 10, 12, 14])
const cancelled = new AbortController()
let started = 0
await mapTeams([1, 2, 3, 4, 5], cancelled.signal, async (row) => {
  started++
  cancelled.abort()
  return row
})
assert.equal(started, 1)
let releaseOld
const key = ref('old')
const scope = effectScope()
const read = scope.run(() =>
  useTeamRead(
    () => key.value,
    () =>
      key.value === 'old'
        ? new Promise((resolve) => {
            releaseOld = resolve
          })
        : Promise.resolve('new'),
  ),
)
key.value = 'new'
await waitTick()
assert.equal(read.data.value, 'new')
releaseOld('old')
await waitTick()
assert.equal(read.data.value, 'new')
key.value = 'old'
const oldAccountRelease = releaseOld
setSessionToken('member-b')
oldAccountRelease('old-account')
await waitTick()
assert.equal(read.data.value, undefined)
scope.stop()
let failedTeam = 2,
  failList = false,
  revoked = false
const conversations = [1, 2, 3, 4].map((id) => ({
  id: 100 + id,
  type: 'TEAM',
  recruitmentId: id,
  status: id === 3 ? 'READ_ONLY' : 'ACTIVE',
}))
request.defaults.adapter = async (config) => {
  await waitTick()
  let data
  if (config.url === '/notifications') data = { records: [], unreadCount: 0 }
  else if (config.url === '/conversations') {
    if (failList) throw new TeamRequestError(50000, 'failure')
    data = conversations
  } else if (config.url.endsWith('/members')) data = revoked ? [] : [{ id: 7, role: 'MEMBER' }]
  else {
    const id = Number(config.url.split('/').at(-1))
    if (id === failedTeam) throw new TeamRequestError(50000, 'single team failure')
    data = { id, ownerId: 2, currentMembers: 2, maxMembers: 3 }
  }
  return { data: { code: 0, data }, status: 200, config }
}
const teamScope = effectScope()
const teams = teamScope.run(() => useMyTeams())
await teams.reload()
assert.equal(teams.data.value.rows.length, 4)
assert.ok(teams.data.value.rows[1].error)
assert.equal(teams.data.value.rows[2].conversation.status, 'READ_ONLY')
failList = true
await teams.reload()
assert.ok(teams.error.value)
assert.equal(teams.data.value, undefined)
failList = false
failedTeam = 0
revoked = true
await teams.reload()
assert.equal(teams.data.value.rows.length, 0)
assert.equal(teams.data.value.removed, true)
teamScope.stop()
const actionScope = effectScope(),
  context = ref('team-1')
const action = actionScope.run(() => useTeamAction(() => context.value))
let release,
  writes = 0
const oldWrite = action.run(
  1,
  () => {
    writes++
    return new Promise((resolve) => {
      release = resolve
    })
  },
  () => 'old success',
)
assert.equal(
  await action.run(
    1,
    () => {
      writes++
      return Promise.resolve()
    },
    () => '',
  ),
  false,
)
assert.equal(writes, 1)
setSessionToken('member-c')
release({})
await oldWrite
assert.equal(action.notice.value, '')
assert.equal(sync.revisions[1], undefined)
const conflictRevision = sync.conversationsRevision
assert.equal(
  await action.run(
    1,
    () => Promise.reject(new TeamRequestError(40900, 'conflict')),
    () => 'bad success',
  ),
  false,
)
assert.equal(action.notice.value, '')
assert.match(action.error.value, /状态已变化/)
assert.equal(sync.conversationsRevision, conflictRevision + 1)
assert.equal(
  await action.run(
    1,
    () => Promise.resolve({ status: 'EXPIRED' }),
    (row) => row.status,
  ),
  true,
)
assert.equal(action.notice.value, 'EXPIRED')
const routeWrite = action.run(
  1,
  () =>
    new Promise((resolve) => {
      release = resolve
    }),
  () => 'wrong route',
)
context.value = 'team-2'
release({})
await routeWrite
assert.equal(action.notice.value, '')
actionScope.stop()
await nextTick()
console.log(
  'PASS: bounded reads, cancellation, stale session/route isolation, partial/list failure, revoked membership, read-only groups, transfer roles and 409 refresh',
)
