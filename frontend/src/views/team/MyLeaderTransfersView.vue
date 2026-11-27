<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TeamMembershipStatus from '@/components/team/TeamMembershipStatus.vue'
import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useTeamRead } from '@/composables/useTeamRead'
import { useTeamAction } from '@/composables/useTeamAction'
import {
  listMyLeaderTransfers,
  acceptLeaderTransfer,
  rejectLeaderTransfer,
} from '@/api/team-management'
import { canRespondToTransfer, transferEffect } from '@/utils/team-member-side'
import { leaderTransferLabels, type LeaderTransfer } from '@/types/team-management'
import { displayDate } from '@/types/recruitment'
const route = useRoute(),
  auth = useAuthStore(),
  sync = useTeamSyncStore()
const { data, loading, error, reload } = useTeamRead(
  () => String(sync.conversationsRevision),
  listMyLeaderTransfers,
)
const { busy, notice, error: actionError, run } = useTeamAction(() => 'leader-transfers')
const updatedTeam = ref<number>()
watch(
  () => sync.sessionRevision,
  () => {
    updatedTeam.value = undefined
  },
)
const confirmation = ref<{ id: number; kind: 'accept' | 'reject' }>()
watch([() => sync.sessionRevision, data], () => {
  confirmation.value = undefined
})
const target = computed(() =>
  typeof route.query.transferId === 'string' ? route.query.transferId : '',
)
const found = computed(() => data.value?.some((row) => String(row.id) === target.value))
watch(
  [target, data],
  async () => {
    if (!found.value) return
    await nextTick()
    document.getElementById(`transfer-${target.value}`)?.scrollIntoView({ block: 'center' })
  },
  { immediate: true },
)
async function respond(row: LeaderTransfer) {
  const choice = confirmation.value
  if (
    busy.value ||
    loading.value ||
    error.value ||
    !choice ||
    choice.id !== row.id ||
    !canRespondToTransfer(row, auth.user?.id)
  )
    return
  confirmation.value = undefined
  const completed = await run(
    row.recruitmentId,
    () => (choice.kind === 'accept' ? acceptLeaderTransfer(row.id) : rejectLeaderTransfer(row.id)),
    (result) =>
      `转交处理结果：${leaderTransferLabels[result.status]}。队伍与会话权限正在重新读取。`,
  )
  if (completed) updatedTeam.value = row.recruitmentId
}
</script>
<template>
  <section class="recruit">
    <header class="page-heading">
      <div>
        <p class="eyebrow">职责交接</p>
        <h1>负责人转交</h1>
        <p>接收者确认前，原负责人继续保留权限与成员资格。</p>
      </div>
      <button :disabled="loading || busy" @click="reload">刷新转交记录</button>
    </header>
    <TeamMembershipStatus v-if="updatedTeam" :recruitment-id="updatedTeam" />
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="actionError" role="alert" class="form-error">{{ actionError }}</p>
    <p v-if="busy" role="status">正在处理转交，请勿重复提交…</p>
    <p v-if="loading" role="status">正在查询转交记录…</p>
    <div v-else-if="error" role="alert">
      <p>{{ notice ? '操作已完成，但记录刷新失败，请刷新，无需重复操作。' : '' }}{{ error }}</p>
      <button @click="reload">重试转交记录</button>
    </div>
    <template v-else-if="data">
      <p v-if="target && !found" role="status">对应转交记录不可用，以下仍展示你的全部转交记录。</p>
      <div v-if="!data.length" class="panel recruit-card">
        <h2>暂无负责人转交记录</h2>
        <RouterLink to="/my-teams">查看我的队伍</RouterLink>
      </div>
      <article
        v-for="row in data"
        :id="`transfer-${row.id}`"
        :key="row.id"
        class="panel recruit-card"
        :class="{ 'application-highlight': String(row.id) === target }"
      >
        <div class="recruit-meta">
          <h2>{{ row.recruitmentTitle }}</h2>
          <strong>{{ leaderTransferLabels[row.status] }}</strong>
        </div>
        <p>
          {{ row.fromUserId === auth.user?.id ? '我发起的转交' : '我收到的转交' }}：{{
            row.fromDisplayName
          }}
          → {{ row.toDisplayName }}
        </p>
        <p>发起时间：{{ displayDate(row.createdAt) }}</p>
        <p v-if="row.processedAt">处理时间：{{ displayDate(row.processedAt) }}</p>
        <p>{{ transferEffect(row) }}</p>
        <p v-if="row.handoverHint">交接提示：{{ row.handoverHint }}</p>
        <div v-if="canRespondToTransfer(row, auth.user?.id)">
          <div
            v-if="confirmation?.id === row.id"
            class="team-confirm"
            role="group"
            aria-label="确认负责人转交"
          >
            <h3>
              {{ confirmation.kind === 'accept' ? '确认接受负责人职责？' : '确认拒绝本次转交？' }}
            </h3>
            <p>
              {{
                confirmation.kind === 'accept'
                  ? transferEffect(row)
                  : '拒绝后原负责人继续留队，队伍角色不会因本次拒绝改变。'
              }}
            </p>
            <p v-if="row.handoverHint">{{ row.handoverHint }}</p>
            <button :disabled="busy" @click="respond(row)">
              {{ confirmation.kind === 'accept' ? '确认接受' : '确认拒绝' }}</button
            ><button :disabled="busy" @click="confirmation = undefined">取消</button>
          </div>
          <div v-else class="recruit-actions">
            <button :disabled="busy" @click="confirmation = { id: row.id, kind: 'accept' }">
              接受转交</button
            ><button :disabled="busy" @click="confirmation = { id: row.id, kind: 'reject' }">
              拒绝转交
            </button>
          </div>
        </div>
        <RouterLink :to="{ name: 'recruitment-detail', params: { id: row.recruitmentId } }"
          >查看最新队伍与成员</RouterLink
        >
      </article>
    </template>
  </section>
</template>
