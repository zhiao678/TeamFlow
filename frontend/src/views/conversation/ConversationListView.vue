<script setup lang="ts">
import { computed, onMounted, onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConversationsStore } from '@/stores/conversations'
import { useTeamSyncStore } from '@/stores/team-sync'
import { teamRoutePath } from '@/router/team-routes'
const conversations = useConversationsStore(),
  sync = useTeamSyncStore()
const route = useRoute(),
  router = useRouter()
const filter = computed(() =>
  route.query.type === 'TEAM' ? 'TEAM' : route.query.type === 'DIRECT' ? 'DIRECT' : 'ALL',
)
const rows = computed(() =>
  conversations.items.filter((item) => filter.value === 'ALL' || item.type === filter.value),
)
const running = ref(true)
let timer: ReturnType<typeof setTimeout> | undefined
async function refresh() {
  await conversations.refresh()
}
function schedule() {
  clearTimeout(timer)
  if (!running.value || document.visibilityState !== 'visible') return
  timer = setTimeout(async () => {
    await refresh()
    schedule()
  }, 5000)
}
function visibilityChanged() {
  clearTimeout(timer)
  if (document.visibilityState === 'visible') void refresh().finally(schedule)
}
onMounted(() => {
  document.addEventListener('visibilitychange', visibilityChanged)
  void refresh().finally(schedule)
})
watch(() => sync.conversationsRevision, refresh)
onScopeDispose(() => {
  running.value = false
  clearTimeout(timer)
  document.removeEventListener('visibilitychange', visibilityChanged)
})
function change(value: string) {
  void router.replace({ path: '/conversations', query: value === 'ALL' ? {} : { type: value } })
}
</script>
<template>
  <section>
    <div class="page-heading">
      <div>
        <p class="eyebrow">一起沟通，高效协作</p>
        <h1>会话</h1>
        <p>未读消息：{{ conversations.unreadCount ?? '待获取' }} · 页面可见时每 5 秒刷新</p>
      </div>
      <el-button :loading="conversations.loading" @click="refresh">刷新</el-button>
    </div>
    <div class="actions" aria-label="会话筛选">
      <el-button
        v-for="item in [
          { key: 'ALL', label: '全部' },
          { key: 'DIRECT', label: '私聊' },
          { key: 'TEAM', label: '队伍群' },
        ]"
        :key="item.key"
        :type="filter === item.key ? 'primary' : 'default'"
        :aria-pressed="filter === item.key"
        @click="change(item.key)"
        >{{ item.label }}</el-button
      >
    </div>
    <p v-if="conversations.error" role="alert" class="form-error">
      {{ conversations.error }} <el-button @click="refresh">重试</el-button>
    </p>
    <p v-if="conversations.loading && !conversations.loaded" role="status">正在加载会话…</p>
    <section v-else-if="conversations.loaded" class="panel">
      <el-empty
        v-if="!rows.length && !conversations.error"
        description="暂无会话，可从招募详情联系负责人，或在成队后进入队伍群。"
      />
      <RouterLink
        v-for="item in rows"
        :key="item.id"
        :to="teamRoutePath('conversation', item.id)"
        class="conversation-row"
        ><span class="avatar">{{
          item.type === 'TEAM' ? '群' : (item.otherUserDisplayName || '用').slice(0, 1)
        }}</span>
        <div class="conversation-summary">
          <h2>
            {{
              item.type === 'TEAM'
                ? `队伍群 · 招募 #${item.recruitmentId}`
                : item.otherUserDisplayName || '私聊用户'
            }}
            <el-tag v-if="item.status === 'READ_ONLY'" size="small" type="info">只读</el-tag>
          </h2>
          <p>{{ item.lastMessage || '暂无消息' }}</p>
          <time>{{ (item.lastMessageAt || item.createdAt).replace('T', ' ').slice(0, 16) }}</time>
        </div>
        <el-tag v-if="item.unreadCount" size="small"
          >{{ item.unreadCount > 99 ? '99+' : item.unreadCount }} 条未读</el-tag
        ></RouterLink
      >
    </section>
  </section>
</template>
<style scoped>
.conversation-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  border-bottom: 1px solid #edf0f6;
  color: #20304b;
}
.conversation-row:hover {
  background: #f8faff;
}
.conversation-summary {
  flex: 1;
  min-width: 0;
}
.conversation-summary p {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 5px 0;
}
.conversation-summary h2 {
  overflow-wrap: anywhere;
}
.avatar {
  flex-shrink: 0;
}
</style>
