<script setup lang="ts">
import { useRoute } from 'vue-router'
import { currentUser } from '@/mocks/data'
import { useNotificationsStore } from '@/stores/notifications'
defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()
const route = useRoute()
const notifications = useNotificationsStore()
</script>

<template>
  <header class="topbar">
    <div class="header-start">
      <button
        class="icon-button"
        :aria-expanded="!collapsed"
        aria-label="切换侧栏"
        @click="$emit('toggle')"
      >
        ☰</button
      ><span class="breadcrumb"
        >工作空间 <span>/</span> <strong>{{ route.meta.title }}</strong></span
      >
    </div>
    <div class="header-end">
      <el-tag type="info" effect="plain" size="small">{{
        route.meta.projectData ? '项目数据已接入' : '模拟数据'
      }}</el-tag
      ><RouterLink
        to="/notifications"
        class="message-link"
        :aria-label="`消息，${notifications.unreadCount} 条未读`"
        >消息<span v-if="notifications.unreadCount" class="unread-dot">{{
          notifications.unreadCount
        }}</span></RouterLink
      ><span class="avatar">张</span><span class="user-name">{{ currentUser.name }}</span>
    </div>
  </header>
</template>
