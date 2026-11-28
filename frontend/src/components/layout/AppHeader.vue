<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import { useConversationsStore } from '@/stores/conversations'
defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationsStore()
const conversations = useConversationsStore()
async function logout() {
  if (await auth.logout()) await router.replace('/login')
}
</script>
<template>
  <header class="topbar">
    <div class="header-start">
      <button
        class="icon-button"
        aria-controls="main-sidebar"
        :aria-expanded="!collapsed"
        aria-label="切换侧栏"
        @click="$emit('toggle')"
      >
        ☰</button
      ><span class="breadcrumb"
        >工作空间 <span>/</span><strong>{{ route.meta.title }}</strong></span
      >
    </div>
    <div class="header-end">
      <RouterLink
        to="/conversations"
        class="message-link"
        :title="conversations.error || '查看聊天会话'"
        >聊天<span
          v-if="auth.token"
          class="unread-dot"
          :aria-label="
            conversations.unreadCount === null
              ? '聊天未读数待获取'
              : `${conversations.unreadCount} 条聊天未读`
          "
          >{{
            conversations.unreadCount === null
              ? '—'
              : conversations.unreadCount > 99
                ? '99+'
                : conversations.unreadCount
          }}</span
        ></RouterLink
      >
      <RouterLink
        to="/notifications"
        class="message-link"
        :title="notifications.countError || '查看通知'"
        >通知<span
          v-if="auth.token"
          class="unread-dot"
          :aria-label="
            notifications.unreadCount === null
              ? '未读数待获取'
              : `${notifications.unreadCount} 条未读`
          "
          >{{
            notifications.unreadCount === null
              ? '—'
              : notifications.unreadCount > 99
                ? '99+'
                : notifications.unreadCount
          }}</span
        ></RouterLink
      ><template v-if="auth.user"
        ><RouterLink
          to="/profile"
          class="message-link header-account"
          :aria-label="auth.user.displayName + '的个人资料'"
          ><span class="avatar">{{ auth.user.displayName.slice(0, 1) || '用户' }}</span
          ><span class="header-display-name">{{ auth.user.displayName }}</span></RouterLink
        ><el-button text :loading="auth.loggingOut" @click="logout">退出登录</el-button></template
      ><template v-else-if="auth.token"
        ><span class="message-link">{{ auth.loading ? '正在验证身份…' : '身份待验证' }}</span
        ><el-button text :loading="auth.loggingOut" @click="logout">退出登录</el-button></template
      ><RouterLink v-else to="/login">登录</RouterLink>
    </div>
  </header>
</template>
