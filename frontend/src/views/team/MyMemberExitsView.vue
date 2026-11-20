<script setup lang="ts">
import { watch } from 'vue'
import { listMyMemberExits } from '@/api/team-management'
import { useTeamRead } from '@/composables/useTeamRead'
import { useTeamSyncStore } from '@/stores/team-sync'
import { memberExitLabels } from '@/types/team-management'
import { displayDate } from '@/types/recruitment'
const sync = useTeamSyncStore()
const { data, loading, error, reload } = useTeamRead(
  () => String(sync.conversationsRevision),
  listMyMemberExits,
)
// 每个已处理状态只广播一次，刷新其他页面的会话/成员权限，不据历史记录推算角色。
const observed = new Map<number, string>()
watch(
  () => sync.sessionRevision,
  () => observed.clear(),
  { flush: 'sync' },
)
watch(data, (rows) => {
  const changed = new Set<number>()
  for (const row of rows ?? []) {
    if (observed.get(row.id) !== row.status && ['APPROVED', 'EXPIRED'].includes(row.status))
      changed.add(row.recruitmentId)
    observed.set(row.id, row.status)
  }
  for (const id of changed) sync.invalidateTeam(id, sync.sessionRevision)
})
</script>
<template>
  <section class="recruit">
    <header class="page-heading">
      <div>
        <p class="eyebrow">成员事务</p>
        <h1>我的退出申请</h1>
        <p>申请提交后仍保留成员资格，只有负责人批准后才离队。</p>
      </div>
      <button :disabled="loading" @click="reload">刷新退出申请</button>
    </header>
    <p v-if="loading" role="status">正在加载退出记录…</p>
    <div v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button @click="reload">重试退出申请</button>
    </div>
    <template v-else-if="data">
      <div v-if="!data.length" class="panel recruit-card">
        <h2>暂无退出申请</h2>
        <RouterLink to="/my-teams">查看我的队伍</RouterLink>
      </div>
      <article v-for="row in data" :key="row.id" class="panel recruit-card">
        <div class="recruit-meta">
          <h2>{{ row.recruitmentTitle }}</h2>
          <strong>{{ memberExitLabels[row.status] }}</strong>
        </div>
        <p>申请时间：{{ displayDate(row.createdAt) }}</p>
        <p>退出原因：{{ row.reason || '未填写' }}</p>
        <p v-if="row.processedAt">处理时间：{{ displayDate(row.processedAt) }}</p>
        <p v-if="row.rejectionReason">拒绝原因：{{ row.rejectionReason }}</p>
        <p v-if="row.handoverHint">交接提示：{{ row.handoverHint }}</p>
        <p v-if="row.status === 'PENDING'">等待负责人审批，目前仍保留成员资格。</p>
        <p v-else-if="row.status === 'APPROVED'">
          此申请已批准；当前成员及群聊资格以重新查询结果为准。
        </p>
        <p v-else-if="row.status === 'EXPIRED'">此记录已失效，不会据此再次改变成员资格。</p>
        <RouterLink
          :to="{
            name: 'recruitment-detail',
            params: { id: row.recruitmentId },
            hash: '#member-exit',
          }"
          >查看队伍与当前成员资格</RouterLink
        >
      </article>
    </template>
  </section>
</template>
