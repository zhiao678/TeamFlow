<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTeamSyncStore } from '@/stores/team-sync'
import { useTeamRead } from '@/composables/useTeamRead'
import { useTeamAction } from '@/composables/useTeamAction'
import { listMyMemberExits, requestMemberExit } from '@/api/team-management'
import { memberExitLabels } from '@/types/team-management'
import type { TeamMember } from '@/types/application'
import { teamRoutes, teamRoutePath } from '@/router/team-routes'
const props = defineProps<{
  recruitmentId: number
  ownerId?: number
  members: TeamMember[]
  ready: boolean
}>()
const auth = useAuthStore(),
  sync = useTeamSyncStore(),
  router = useRouter()
const reason = ref(''),
  confirming = ref(false)
const { busy, notice, error: actionError, run } = useTeamAction(() => String(props.recruitmentId))
const { data, loading, error, reload } = useTeamRead(
  () => `${props.recruitmentId}:${sync.revisions[props.recruitmentId] ?? 0}`,
  (signal) => (auth.token ? listMyMemberExits(signal) : Promise.resolve([])),
)
watch([() => props.recruitmentId, () => sync.sessionRevision], () => {
  reason.value = ''
  confirming.value = false
})
const member = computed(() => props.members.find((row) => row.id === auth.user?.id))
const owner = computed(() => auth.user?.id === props.ownerId || member.value?.role === 'LEADER')
const records = computed(() =>
  (data.value ?? []).filter((row) => row.recruitmentId === props.recruitmentId),
)
const pending = computed(() => records.value.find((row) => row.status === 'PENDING'))
const latest = computed(
  () => [...records.value].sort((a, b) => b.createdAt.localeCompare(a.createdAt) || b.id - a.id)[0],
)
const canRequest = computed(
  () =>
    !!auth.user &&
    props.ready &&
    member.value?.role === 'MEMBER' &&
    !owner.value &&
    !loading.value &&
    !error.value &&
    data.value !== undefined &&
    !pending.value,
)
async function submit() {
  if (!canRequest.value || busy.value || reason.value.length > 255 || !confirming.value) return
  confirming.value = false
  if (
    await run(
      props.recruitmentId,
      () => requestMemberExit(props.recruitmentId, reason.value),
      (result) =>
        result.status === 'PENDING'
          ? '退出申请已提交，等待负责人审批。批准前你仍是队伍成员。'
          : `服务端返回：${memberExitLabels[result.status]}，请查看最新记录。`,
    )
  )
    reason.value = ''
}
</script>
<template>
  <section id="member-exit" class="panel recruit-card">
    <h2>队伍成员操作</h2>
    <p v-if="notice" role="status">{{ notice }}</p>
    <p v-if="actionError" role="alert" class="form-error">{{ actionError }}</p>
    <p v-if="busy" role="status">正在提交退出申请…</p>
    <p v-if="!ready">成员身份尚未确认，请等待详情和成员加载完成；失败时请在上方重试。</p>
    <template v-else-if="owner">
      <p>你是负责人，不能直接申请退出。请先在队伍管理中指定继任者，待对方接受转交。</p>
      <RouterLink
        v-if="router.hasRoute(teamRoutes.teamManagement.name)"
        :to="teamRoutePath('teamManagement', recruitmentId)"
        >前往队伍管理</RouterLink
      >
      <p v-else>负责人发起转交入口待管理模块接入。</p>
      <RouterLink :to="teamRoutes.leaderTransfers.path">查看负责人转交记录</RouterLink>
    </template>
    <p v-else-if="!member">你当前不是该队伍的正式成员，无需申请退出。</p>
    <template v-else>
      <p v-if="loading" role="status">正在查询退出申请…</p>
      <div v-else-if="error" role="alert">
        <p>{{ notice ? '操作结果已保留，退出记录刷新失败。' : '' }}{{ error }}</p>
        <button @click="reload">重试退出记录</button>
      </div>
      <template v-else>
        <p v-if="pending">等待负责人审批。审批通过前仍保留成员资格、人数及群聊入口。</p>
        <p v-else-if="latest">
          最近一次退出申请：{{ memberExitLabels[latest.status]
          }}<span v-if="latest.rejectionReason">；拒绝原因：{{ latest.rejectionReason }}</span>
        </p>
        <p v-if="pending?.handoverHint">交接提示：{{ pending.handoverHint }}</p>
        <form
          v-if="canRequest"
          class="recruit-application-form"
          @submit.prevent="confirming = true"
        >
          <label
            >退出原因（选填）<textarea v-model="reason" maxlength="255" :disabled="busy" /><small
              >{{ reason.length }} / 255</small
            ></label
          >
          <button v-if="!confirming" :disabled="busy" type="submit">申请退出队伍</button>
          <div v-else class="team-confirm" role="group" aria-label="确认退出申请">
            <p>提交后需要负责人审批，当前不会离队。确认申请退出吗？</p>
            <button type="button" :disabled="busy" @click="submit">确认提交退出申请</button
            ><button type="button" :disabled="busy" @click="confirming = false">取消</button>
          </div>
        </form>
      </template>
      <RouterLink :to="teamRoutes.memberExits.path">查看我的退出申请</RouterLink>
    </template>
  </section>
</template>
