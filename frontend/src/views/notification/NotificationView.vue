<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNotificationsStore } from '@/stores/notifications'
import { sessionToken } from '@/api/session'
import { notificationTarget } from '@/utils/notification-target'
import type { Notification } from '@/types/notification'
const route = useRoute(),
  router = useRouter()
const notifications = useNotificationsStore()
function positive(value: unknown, fallback: number) {
  const n = Number(value)
  return Number.isSafeInteger(n) && n > 0 ? n : fallback
}
const query = computed(() => ({
  page: positive(route.query.page, 1),
  pageSize: Math.min(100, positive(route.query.pageSize, 20)),
  unreadOnly: route.query.unreadOnly === 'true',
}))
const pages = computed(() => Math.max(1, Math.ceil(notifications.total / notifications.pageSize)))
const labels: Record<string, string> = {
  APPLICATION_SUBMITTED: '收到申请',
  APPLICATION_WITHDRAWN: '申请撤回',
  APPLICATION_ACCEPTED: '申请通过',
  APPLICATION_REJECTED: '申请未通过',
  APPLICATION_EXPIRED: '申请失效',
}
async function reload() {
  const path = route.fullPath
  const ok = await notifications.load(query.value)
  if (ok && path === route.fullPath && notifications.page !== query.value.page) {
    await router.replace({
      path: '/notifications',
      query: { ...route.query, page: String(notifications.page) },
    })
  }
}
watch(
  [query, sessionToken],
  () => {
    void reload()
  },
  { immediate: true },
)
function change(unreadOnly: boolean, page = 1, pageSize = query.value.pageSize) {
  void router.push({
    path: '/notifications',
    query: {
      unreadOnly: unreadOnly ? 'true' : undefined,
      page: page > 1 ? String(page) : undefined,
      pageSize: pageSize === 20 ? undefined : String(pageSize),
    },
  })
}
watch(
  () => [notifications.page, notifications.loading, notifications.marking],
  () => {
    if (
      !notifications.loading &&
      !notifications.marking &&
      !notifications.error &&
      sessionToken.value &&
      notifications.unreadOnly === query.value.unreadOnly &&
      notifications.page !== query.value.page
    ) {
      void router.replace({
        path: '/notifications',
        query: { ...route.query, page: String(notifications.page) },
      })
    }
  },
)
function target(item: Notification) {
  return notificationTarget(
    item,
    router.getRoutes().map((record) => record.path),
  )
}
async function view(item: Notification) {
  const destination = target(item)
  if (!destination.path) return
  const token = sessionToken.value
  if (!item.read) await notifications.markRead(item.id)
  // 标记失败不阻止查看；若会话已失效，则由统一 401 逻辑处理。
  if (sessionToken.value && sessionToken.value === token) {
    await router.push({ path: destination.path, query: destination.query })
  }
}
</script>

<template>
  <section class="notification-center">
    <div class="page-heading">
      <div>
        <p class="eyebrow">团队动态，及时掌握</p>
        <h1>站内通知</h1>
        <p>全部未读：{{ notifications.unreadCount ?? '待获取' }}</p>
      </div>
      <div class="actions">
        <el-button
          :loading="notifications.loading"
          :disabled="notifications.marking"
          @click="reload"
          >刷新</el-button
        ><el-button
          :loading="notifications.marking"
          :disabled="
            notifications.loading ||
            notifications.unreadCount === 0 ||
            notifications.unreadCount === null
          "
          @click="notifications.markAllRead()"
          >全部标为已读</el-button
        >
      </div>
    </div>
    <div class="notification-toolbar">
      <div class="actions" aria-label="通知筛选">
        <el-button
          :type="!query.unreadOnly ? 'primary' : 'default'"
          :aria-pressed="!query.unreadOnly"
          :disabled="notifications.marking"
          @click="change(false)"
          >全部</el-button
        ><el-button
          :type="query.unreadOnly ? 'primary' : 'default'"
          :aria-pressed="query.unreadOnly"
          :disabled="notifications.marking"
          @click="change(true)"
          >未读</el-button
        >
      </div>
      <label
        >每页
        <select
          :value="query.pageSize"
          :disabled="notifications.marking"
          @change="change(query.unreadOnly, 1, Number(($event.target as HTMLSelectElement).value))"
        >
          <option :value="20">20 条</option>
          <option :value="50">50 条</option>
          <option :value="100">100 条</option>
        </select></label
      >
    </div>
    <p v-if="notifications.notice" role="status">{{ notifications.notice }}</p>
    <p v-if="notifications.actionError" role="alert" class="form-error">
      {{ notifications.actionError }}
    </p>
    <p v-if="notifications.countError" role="alert">
      未读数获取失败。<el-button
        :loading="notifications.countLoading"
        @click="notifications.refreshUnreadCount()"
        >重试未读数</el-button
      >
    </p>
    <p v-if="notifications.loading" role="status">正在加载通知…</p>
    <section v-else-if="notifications.error" class="panel profile-panel" role="alert">
      <p>{{ notifications.error }}</p>
      <el-button @click="reload">重新加载</el-button>
    </section>
    <template v-else>
      <section class="panel" :aria-busy="notifications.marking">
        <el-empty
          v-if="!notifications.items.length"
          :description="query.unreadOnly ? '暂无未读通知' : '暂无通知'"
        />
        <article
          v-for="item in notifications.items"
          :key="item.id"
          class="notification-row"
          :class="{ unread: !item.read }"
        >
          <span class="notification-icon" aria-hidden="true">♧</span>
          <div class="row-main">
            <div class="notification-item-heading">
              <h2>{{ item.title }}</h2>
              <el-tag v-if="!item.read" size="small">未读</el-tag
              ><el-tag v-else size="small" type="info">已读</el-tag>
            </div>
            <p>{{ item.content }}</p>
            <time :datetime="item.createdAt"
              >{{ item.createdAt.replace('T', ' ').slice(0, 16) }}（北京时间）</time
            >
            <p class="notification-event">{{ labels[item.eventType] || '其他通知' }}</p>
            <p v-if="target(item).hint">{{ target(item).hint }}</p>
            <div class="actions">
              <el-button
                v-if="target(item).path"
                text
                type="primary"
                :disabled="notifications.marking"
                @click="view(item)"
                >{{ target(item).label }}</el-button
              >
              <el-button
                v-if="!item.read"
                text
                type="primary"
                :disabled="notifications.marking"
                @click="notifications.markRead(item.id)"
                >标为已读</el-button
              >
            </div>
          </div>
        </article>
      </section>
      <nav v-if="notifications.total > 0" class="notification-pagination" aria-label="通知分页">
        <span>共 {{ notifications.total }} 条 · {{ notifications.page }} / {{ pages }} 页</span
        ><el-button
          :disabled="notifications.marking || notifications.page <= 1"
          @click="change(query.unreadOnly, notifications.page - 1)"
          >上一页</el-button
        ><el-button
          :disabled="notifications.marking || notifications.page >= pages"
          @click="change(query.unreadOnly, notifications.page + 1)"
          >下一页</el-button
        >
      </nav>
    </template>
  </section>
</template>

<style scoped>
.notification-toolbar,
.notification-pagination,
.notification-item-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.notification-toolbar {
  justify-content: space-between;
  margin-bottom: 20px;
}
.notification-toolbar label,
.notification-pagination {
  font-size: 13px;
  color: #65718a;
}
.notification-toolbar select {
  padding: 7px;
  border: 1px solid #dce3ef;
  border-radius: 6px;
  background: white;
}
.notification-pagination {
  justify-content: flex-end;
  margin-top: 20px;
}
.notification-event {
  font-size: 12px;
}
.row-main {
  overflow-wrap: anywhere;
}
</style>
