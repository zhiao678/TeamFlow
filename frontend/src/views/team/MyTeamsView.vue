<script setup lang="ts">
import { useMyTeams } from '@/composables/useMyTeams'
import { memberRoleLabels } from '@/types/application'
import { statusLabels } from '@/types/recruitment'
import TeamConversationEntry from '@/components/team/TeamConversationEntry.vue'
const { data, loading, error, reload } = useMyTeams()
</script>
<template>
  <section class="recruit">
    <header class="page-heading">
      <div>
        <p class="eyebrow">我的协作空间</p>
        <h1>我的队伍</h1>
        <p>展示已确认成队且你有权访问队伍群的队伍。</p>
      </div>
      <button :disabled="loading" @click="reload">刷新队伍</button>
    </header>
    <p v-if="loading" role="status">正在加载队伍、角色和成员信息…</p>
    <div v-else-if="error" role="alert">
      <p>{{ error }}</p>
      <button @click="reload">重试队伍列表</button>
    </div>
    <template v-else-if="data">
      <p v-if="data.removed" role="status">成员或会话资格已变化，已刷新列表并移除失效入口。</p>
      <div v-if="!data.rows.length" class="panel recruit-card">
        <h2>暂无可访问的已成队队伍</h2>
        <p>仍在招募中的队伍可从招募详情查看，负责人确认成队后会出现在这里。</p>
        <RouterLink to="/recruitments">前往招募大厅</RouterLink>
      </div>
      <div v-else class="recruit-grid">
        <article v-for="row in data.rows" :key="row.conversation.id" class="panel recruit-card">
          <template v-if="row.error"
            ><h2>队伍信息暂不可用</h2>
            <p role="alert">{{ row.error }}</p>
            <button @click="reload">重试队伍信息</button></template
          >
          <template v-else-if="row.recruitment && row.member">
            <div class="recruit-meta">
              <span>{{ memberRoleLabels[row.member.role] }}</span
              ><strong>{{ statusLabels[row.recruitment.status] }}</strong>
            </div>
            <h2>{{ row.recruitment.title }}</h2>
            <p>负责人：{{ row.recruitment.ownerDisplayName }}</p>
            <p>{{ row.recruitment.currentMembers }} / {{ row.recruitment.maxMembers }} 人</p>
            <p v-if="row.conversation.status === 'READ_ONLY'">
              队伍群当前只读，可查看历史消息，不能发送消息。
            </p>
            <div class="recruit-actions">
              <RouterLink :to="{ name: 'recruitment-detail', params: { id: row.recruitment.id } }"
                >查看队伍与成员</RouterLink
              ><TeamConversationEntry
                :recruitment-id="row.recruitment.id"
                :read-only="row.conversation.status === 'READ_ONLY'"
              />
            </div>
            <RouterLink
              v-if="row.member.role === 'MEMBER'"
              :to="{
                name: 'recruitment-detail',
                params: { id: row.recruitment.id },
                hash: '#member-exit',
              }"
              >申请退出队伍</RouterLink
            >
          </template>
        </article>
      </div>
    </template>
  </section>
</template>
