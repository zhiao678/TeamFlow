<script setup lang="ts">
import { useNotificationsStore } from '@/stores/notifications'
const notifications = useNotificationsStore()
</script>
<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">团队动态，及时掌握</p>
      <h1>消息</h1>
      <p>你有 {{ notifications.unreadCount }} 条未读消息。已读状态仅在本次访问中保留。</p>
    </div>
    <el-button :disabled="notifications.unreadCount === 0" @click="notifications.markAllRead()"
      >全部标为已读</el-button
    >
  </div>
  <section class="panel">
    <article
      v-for="item in notifications.items"
      :key="item.id"
      class="notification-row"
      :class="{ unread: !item.read }"
    >
      <span class="notification-icon">♧</span>
      <div class="row-main">
        <h2>{{ item.title }} <el-tag v-if="!item.read" size="small">未读</el-tag></h2>
        <p>{{ item.content }}</p>
        <time>{{ item.createdAt.replace('T', ' ').slice(0, 16) }}</time>
        <div class="actions">
          <RouterLink :to="item.target"
            >{{ item.target === '/tasks' ? '查看任务' : '查看项目' }} →</RouterLink
          ><el-button v-if="!item.read" text type="primary" @click="notifications.markRead(item.id)"
            >标为已读</el-button
          >
        </div>
      </div>
    </article>
  </section>
</template>
