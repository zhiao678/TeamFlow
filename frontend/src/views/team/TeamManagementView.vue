<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeamConversationEntry from '@/components/team/TeamConversationEntry.vue'
import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useTeamRead } from '@/composables/useTeamRead'
import { useTeamAction } from '@/composables/useTeamAction'
import { getRecruitment } from '@/api/recruitment-query'
import { listRecruitmentMembers } from '@/api/members'
import {
  approveMemberExit,
  formTeam,
  listMyLeaderTransfers,
  listRecruitmentExits,
  rejectMemberExit,
  removeTeamMember,
  requestLeadershipExit,
} from '@/api/team-management'
import { teamRoutes } from '@/router/team-routes'
import { memberRoleLabels } from '@/types/application'
import { displayDate, displayStatus, statusLabels } from '@/types/recruitment'
import { leaderTransferLabels, memberExitLabels } from '@/types/team-management'
import type { LeaderTransfer, MemberExitRequest } from '@/types/team-management'
import type { Recruitment } from '@/types/recruitment'
import type { TeamMember } from '@/types/application'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const sync = useTeamSyncStore()

const rawId = computed(() => String(route.params.id ?? ''))
const id = computed(() => Number(rawId.value))
const validId = computed(() => /^[1-9]\d*$/.test(rawId.value) && Number.isSafeInteger(id.value))

const { data, loading, error, reload } = useTeamRead(
  () => `${id.value}:${sync.revisions[id.value] ?? 0}`,
  async (signal) => {
    const recruitment = await getRecruitment(id.value, signal)
    const members = await listRecruitmentMembers(id.value, signal)
    // 负责人专属接口只对本人请求，非负责人看到的是清晰的“无权限”页面，而不是 403 报错。
    if (recruitment.ownerId !== auth.user?.id) {
      return { recruitment, members, exits: [], transfers: [] }
    }
    const exits = await listRecruitmentExits(id.value, signal)
    const transfers = await listMyLeaderTransfers(signal)
    return {
      recruitment,
      members,
      exits,
      transfers: transfers.filter((row) => row.recruitmentId === id.value),
    }
  },
)

const {
  busy,
  notice,
  error: actionError,
  run,
} = useTeamAction(() => `team-management-${rawId.value}`)

const pendingKey = ref('')
const confirming = ref<{
  kind: 'form-team' | 'remove' | 'approve' | 'reject' | 'leader-exit'
  id?: number
  name?: string
}>()
const rejectReason = ref('')
const successorId = ref('')

const recruitment = computed<Recruitment | undefined>(() => data.value?.recruitment)
const members = computed<TeamMember[]>(() => data.value?.members ?? [])
const exits = computed<MemberExitRequest[]>(() => data.value?.exits ?? [])
const transfers = computed<LeaderTransfer[]>(() => data.value?.transfers ?? [])
const pendingExits = computed(() => exits.value.filter((row) => row.status === 'PENDING'))
const pendingTransfer = computed(() => transfers.value.find((row) => row.status === 'PENDING'))
const isOwner = computed(() => !!recruitment.value && auth.user?.id === recruitment.value.ownerId)
const recruitId = computed(() => recruitment.value?.id ?? 0)
const candidates = computed(() =>
  members.value.filter((row) => row.role === 'MEMBER' && row.id !== recruitment.value?.ownerId),
)
const successor = computed(() => members.value.find((row) => String(row.id) === successorId.value))
const canFormTeam = computed(() => isOwner.value && recruitment.value?.status === 'RECRUITING')
const canManage = computed(
  () =>
    isOwner.value &&
    (recruitment.value?.status === 'RECRUITING' || recruitment.value?.status === 'TEAMED'),
)

watch([() => rawId.value, () => sync.sessionRevision], () => {
  confirming.value = undefined
  pendingKey.value = ''
  rejectReason.value = ''
  successorId.value = ''
})

function openConfirm(
  kind: 'form-team' | 'remove' | 'approve' | 'reject' | 'leader-exit',
  row?: { id: number; name: string },
) {
  if (busy.value) return
  confirming.value = { kind, id: row?.id, name: row?.name }
  rejectReason.value = ''
}

function cancelConfirm() {
  confirming.value = undefined
  rejectReason.value = ''
}

/** 写请求成功后由 team-sync 广播重新查询，页面按服务端实际数据刷新，不本地改人数。 */
async function execute(
  key: string,
  operation: () => Promise<unknown>,
  message: (result: unknown) => string,
) {
  if (busy.value || !recruitId.value) return
  pendingKey.value = key
  confirming.value = undefined
  await run(recruitId.value, operation, message)
  pendingKey.value = ''
}

const confirmFormTeam = () =>
  execute(
    'form-team',
    () => formTeam(recruitId.value),
    () => '已确认成队：待处理申请已失效，队伍群已创建，正在刷新队伍状态。',
  )

const confirmRemove = (member: TeamMember) =>
  execute(
    `remove:${member.id}`,
    () => removeTeamMember(recruitId.value, member.id),
    () => `已移除 ${member.displayName}，对方不再拥有队伍群访问资格，正在刷新人数与名额。`,
  )

const confirmApprove = (row: MemberExitRequest) =>
  execute(
    `approve:${row.id}`,
    () => approveMemberExit(row.id),
    (result) => {
      const status = (result as MemberExitRequest).status
      return status === 'APPROVED'
        ? `已批准 ${row.userDisplayName} 退出，成员资格与群权限已撤销。`
        : `申请状态为“${memberExitLabels[status]}”，成员资格未变化，已按服务端结果刷新。`
    },
  )

const confirmReject = (row: MemberExitRequest) =>
  execute(
    `reject:${row.id}`,
    () => rejectMemberExit(row.id, rejectReason.value.trim()),
    (result) => {
      const status = (result as MemberExitRequest).status
      return status === 'REJECTED'
        ? `已拒绝 ${row.userDisplayName} 的退出申请，成员继续留队。`
        : `申请状态为“${memberExitLabels[status]}”，成员资格未变化，已按服务端结果刷新。`
    },
  )

const confirmLeaderExit = () => {
  if (!successorId.value) return
  const target = successor.value
  return execute(
    'leader-exit',
    () => requestLeadershipExit(recruitId.value, Number(successorId.value)),
    () => `已向 ${target?.displayName ?? '继任者'} 发起负责人转交，对方确认前你仍保留全部权限。`,
  )
}
</script>

<template>
  <section class="recruit">
    <p>
      <RouterLink :to="{ name: 'my-recruitments' }">← 返回我的招募</RouterLink>
      <template v-if="validId">
        ·
        <RouterLink :to="{ name: 'application-review', params: { id } }">申请管理</RouterLink>
        ·
        <RouterLink :to="{ name: 'recruitment-detail', params: { id } }">查看招募详情</RouterLink>
      </template>
    </p>

    <header>
      <p class="eyebrow">招募管理</p>
      <h1>队伍管理</h1>
      <p v-if="recruitment">
        《{{ recruitment.title }}》 · {{ displayStatus(recruitment) }} · 当前
        {{ recruitment.currentMembers }} / {{ recruitment.maxMembers }} 人，剩余
        {{ recruitment.remainingSlots }} 席
      </p>
    </header>

    <p v-if="!validId" role="alert">招募编号无效，请从“我的招募”重新进入。</p>
    <p v-else-if="loading" role="status">正在加载队伍、成员与退出申请…</p>
    <section v-else-if="error" class="panel recruit-card" role="alert">
      <p>{{ notice ? '上一步操作结果已保留，但队伍数据刷新失败。' : '' }}{{ error }}</p>
      <button type="button" @click="reload">重试</button>
    </section>

    <section v-else-if="!isOwner" class="panel recruit-card" role="alert">
      <h2>无权管理该队伍</h2>
      <p>只有当前负责人可以确认成队、移除成员、审批退出和发起负责人转交。</p>
      <RouterLink :to="{ name: 'recruitment-detail', params: { id } }">查看招募详情</RouterLink>
    </section>

    <template v-else-if="recruitment">
      <p v-if="notice" class="notice" role="status">{{ notice }}</p>
      <p v-if="actionError" class="form-error" role="alert">{{ actionError }}</p>
      <p v-if="busy" role="status">正在处理，请勿重复提交…</p>

      <section class="panel recruit-card">
        <div class="recruit-meta">
          <h2>队伍状态</h2>
          <strong>{{ statusLabels[recruitment.status] }}</strong>
        </div>
        <p>
          负责人：{{ recruitment.ownerDisplayName }} · 截止时间：{{
            displayDate(recruitment.deadlineAt)
          }}
        </p>

        <template v-if="recruitment.status === 'RECRUITING'">
          <p v-if="pendingTransfer" class="notice">
            已发起负责人转交（待
            {{ pendingTransfer.toDisplayName }} 确认）。确认前你仍是负责人，仍可处理队伍事务。
          </p>
          <div v-if="canFormTeam" class="recruit-actions">
            <button
              class="primary"
              type="button"
              :disabled="busy"
              @click="openConfirm('form-team')"
            >
              确认成队
            </button>
            <span class="hint">允许未满员成队；确认后不能继续接受新申请。</span>
          </div>
          <div
            v-if="confirming?.kind === 'form-team'"
            class="recruit-confirm"
            role="group"
            aria-label="确认成队"
          >
            <p>
              确认成队后立即生效：所有待处理入队申请变为“已失效”，并创建唯一队伍群。当前
              {{ recruitment.currentMembers }} / {{ recruitment.maxMembers }} 人，未满员也可以成队。
            </p>
            <div class="recruit-actions">
              <button class="primary" type="button" :disabled="busy" @click="confirmFormTeam">
                确认成队
              </button>
              <button type="button" :disabled="busy" @click="cancelConfirm">取消</button>
            </div>
          </div>
        </template>

        <template v-else-if="recruitment.status === 'TEAMED'">
          <p>队伍已确认成队，招募内容不能再修改。</p>
          <TeamConversationEntry :recruitment-id="recruitment.id" />
        </template>

        <p v-else-if="recruitment.status === 'CLOSED'">
          招募已关闭，不能再成队或接受新申请，成员与退出记录仍可查看。
        </p>
        <p v-else>草稿状态没有队伍，发布招募后才能确认成队。</p>
      </section>

      <section class="panel recruit-card">
        <div class="recruit-meta">
          <h2>正式成员</h2>
          <span>{{ members.length }} 人</span>
        </div>
        <p v-if="!members.length" class="hint">还没有正式成员。</p>
        <ul v-else class="recruit-member-list">
          <li v-for="member in members" :key="member.id" class="recruit-member">
            <img
              v-if="member.avatarUrl"
              class="recruit-avatar"
              :src="member.avatarUrl"
              :alt="`${member.displayName}的头像`"
            />
            <span v-else class="recruit-avatar recruit-avatar-text" aria-hidden="true">
              {{ member.displayName.slice(0, 1) || '用' }}
            </span>
            <span class="recruit-member-name">{{ member.displayName }}</span>
            <span class="recruit-member-role">{{ memberRoleLabels[member.role] }}</span>
            <span class="recruit-member-date">{{ displayDate(member.joinedAt) }} 加入</span>

            <span v-if="member.id === recruitment.ownerId" class="hint">
              负责人不能移除自己，请先发起负责人转交。
            </span>
            <template v-else>
              <button
                v-if="canManage"
                type="button"
                class="danger"
                :disabled="busy"
                @click="openConfirm('remove', { id: member.id, name: member.displayName })"
              >
                移除
              </button>
              <div
                v-if="confirming?.kind === 'remove' && confirming.id === member.id"
                class="recruit-confirm"
                role="group"
                aria-label="确认移除成员"
              >
                <p>
                  确认移除
                  {{
                    member.displayName
                  }}？移除后对方立即失去队伍群访问资格，人数与剩余名额会按服务端结果重新计算。
                </p>
                <div class="recruit-actions">
                  <button
                    class="danger"
                    type="button"
                    :disabled="busy"
                    @click="confirmRemove(member)"
                  >
                    确认移除
                  </button>
                  <button type="button" :disabled="busy" @click="cancelConfirm">取消</button>
                </div>
              </div>
            </template>
          </li>
        </ul>
      </section>

      <section class="panel recruit-card">
        <div class="recruit-meta">
          <h2>退出申请审批</h2>
          <span>{{ pendingExits.length }} 条待审批</span>
        </div>
        <p v-if="!exits.length" class="hint">还没有成员提交退出申请。</p>
        <div v-else class="recruit-app-list">
          <article v-for="row in exits" :key="row.id" class="recruit-app">
            <div class="recruit-meta">
              <strong>{{ row.userDisplayName }}</strong>
              <span class="recruit-status">{{ memberExitLabels[row.status] }}</span>
            </div>
            <dl class="recruit-facts">
              <div>
                <dt>退出原因</dt>
                <dd>{{ row.reason || '未填写' }}</dd>
              </div>
              <div>
                <dt>提交时间</dt>
                <dd>{{ displayDate(row.createdAt) }}</dd>
              </div>
              <div v-if="row.processedAt">
                <dt>处理时间</dt>
                <dd>{{ displayDate(row.processedAt) }}</dd>
              </div>
              <div v-if="row.rejectionReason">
                <dt>拒绝原因</dt>
                <dd>{{ row.rejectionReason }}</dd>
              </div>
              <div v-if="row.handoverHint">
                <dt>交接提示</dt>
                <dd>{{ row.handoverHint }}</dd>
              </div>
            </dl>
            <div v-if="row.status === 'PENDING'" class="recruit-actions">
              <button
                class="primary"
                type="button"
                :disabled="busy"
                @click="openConfirm('approve', { id: row.id, name: row.userDisplayName })"
              >
                批准退出
              </button>
              <button
                type="button"
                :disabled="busy"
                @click="openConfirm('reject', { id: row.id, name: row.userDisplayName })"
              >
                拒绝
              </button>
              <span
                v-if="pendingKey === `approve:${row.id}` || pendingKey === `reject:${row.id}`"
                class="hint"
              >
                处理中…
              </span>
            </div>

            <div
              v-if="confirming?.kind === 'approve' && confirming.id === row.id"
              class="recruit-confirm"
              role="group"
              aria-label="确认批准退出"
            >
              <p>
                批准后
                {{ row.userDisplayName }} 立即离队并失去队伍群访问资格，人数与名额按服务端结果刷新。
              </p>
              <div class="recruit-actions">
                <button class="primary" type="button" :disabled="busy" @click="confirmApprove(row)">
                  确认批准
                </button>
                <button type="button" :disabled="busy" @click="cancelConfirm">取消</button>
              </div>
            </div>
            <div
              v-if="confirming?.kind === 'reject' && confirming.id === row.id"
              class="recruit-confirm"
              role="group"
              aria-label="确认拒绝退出"
            >
              <p>拒绝后 {{ row.userDisplayName }} 继续留队。拒绝原因会展示给本人，可留空。</p>
              <label>
                拒绝原因（可选）
                <input v-model="rejectReason" maxlength="255" placeholder="例如：请先沟通分工" />
              </label>
              <small class="hint">{{ rejectReason.length }} / 255</small>
              <div class="recruit-actions">
                <button class="danger" type="button" :disabled="busy" @click="confirmReject(row)">
                  确认拒绝
                </button>
                <button type="button" :disabled="busy" @click="cancelConfirm">取消</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="panel recruit-card">
        <div class="recruit-meta">
          <h2>负责人转交</h2>
          <RouterLink
            v-if="router.hasRoute(teamRoutes.leaderTransfers.name)"
            :to="{ name: teamRoutes.leaderTransfers.name }"
          >
            查看转交记录
          </RouterLink>
        </div>
        <template v-if="pendingTransfer">
          <p class="notice">
            已向 {{ pendingTransfer.toDisplayName }} 发起转交（{{
              leaderTransferLabels[pendingTransfer.status]
            }}）。对方接受前你仍保留负责人权限与成员资格。
          </p>
          <p v-if="pendingTransfer.handoverHint">交接提示：{{ pendingTransfer.handoverHint }}</p>
        </template>
        <template v-else>
          <p class="hint">
            负责人不能直接走成员退出流程。选择一名正式成员作为继任者，对方确认后你才会离队。
          </p>
          <p v-if="!candidates.length" class="hint">
            当前没有可接任的普通成员，先招募并接受申请后再转交。
          </p>
          <template v-else>
            <label class="recruit-select">
              继任者
              <select v-model="successorId" :disabled="busy">
                <option value="" disabled>请选择继任者</option>
                <option v-for="member in candidates" :key="member.id" :value="String(member.id)">
                  {{ member.displayName }}
                </option>
              </select>
            </label>
            <div class="recruit-actions">
              <button
                type="button"
                class="primary"
                :disabled="busy || !successorId"
                @click="openConfirm('leader-exit')"
              >
                发起负责人转交
              </button>
              <span v-if="pendingKey === 'leader-exit'" class="hint">提交中…</span>
            </div>
          </template>
          <div
            v-if="confirming?.kind === 'leader-exit'"
            class="recruit-confirm"
            role="group"
            aria-label="确认发起转交"
          >
            <p>
              确认向
              {{ successor?.displayName }}
              发起负责人转交？对方确认前你仍是负责人并保留全部权限；对方接受后转交生效，你会离队。
            </p>
            <div class="recruit-actions">
              <button
                class="primary"
                type="button"
                :disabled="busy || !successorId"
                @click="confirmLeaderExit"
              >
                确认发起
              </button>
              <button type="button" :disabled="busy" @click="cancelConfirm">取消</button>
            </div>
          </div>
        </template>
      </section>
    </template>
  </section>
</template>
