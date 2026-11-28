<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useConversationsStore } from '@/stores/conversations'
import { homeRoute } from '@/router/home'
import { teamRoutes } from '@/router/team-routes'

defineProps<{ collapsed: boolean }>()
defineEmits<{ navigate: [] }>()

const route = useRoute()
const conversations = useConversationsStore()
const groups = [
  {
    label: '招募',
    links: [
      { to: '/recruitments', label: '招募大厅', icon: '◎' },
      { to: '/my-recruitments', label: '我的招募', icon: '✎' },
      { to: '/my-applications', label: '我的申请', icon: '☑' },
      { to: '/my-favorites', label: '我的收藏', icon: '☆' },
    ],
  },
  {
    label: '队伍与沟通',
    links: [
      { to: teamRoutes.myTeams.path, label: '我的队伍', icon: '♧' },
      { to: teamRoutes.conversations.path, label: '聊天会话', icon: '☏' },
      { to: teamRoutes.memberExits.path, label: '我的退出申请', icon: '↪' },
      { to: teamRoutes.leaderTransfers.path, label: '负责人转交', icon: '⇄' },
    ],
  },
  {
    label: '项目协作',
    links: [{ to: '/projects', label: '项目列表', icon: '▣' }],
  },
  {
    label: '个人',
    links: [{ to: '/profile', label: '个人资料', icon: '◉' }],
  },
]

function isActive(path: string) {
  return route.matched.some((record) => record.path === path || record.path.startsWith(path + '/'))
}
</script>

<template>
  <aside id="main-sidebar" class="sidebar" :class="{ collapsed }">
    <RouterLink
      :to="homeRoute"
      class="brand"
      aria-label="TeamFlow 招募大厅"
      @click="$emit('navigate')"
    >
      <span class="brand-mark" aria-hidden="true">▦</span>
      <span class="nav-text">TeamFlow</span>
    </RouterLink>
    <nav aria-label="主导航">
      <section
        v-for="group in groups"
        :key="group.label"
        class="nav-group"
        :aria-label="group.label"
      >
        <h2 class="nav-caption nav-text">{{ group.label }}</h2>
        <RouterLink
          v-for="link in group.links"
          :key="link.to"
          :to="link.to"
          :title="link.label"
          :aria-label="link.label"
          class="nav-item"
          :class="{ 'nav-item-active': isActive(link.to) }"
          @click="$emit('navigate')"
        >
          <span class="nav-icon" aria-hidden="true">{{ link.icon }}</span>
          <span class="nav-text">{{ link.label }}</span>
          <span
            v-if="link.to === teamRoutes.conversations.path && conversations.unreadCount !== null"
            class="nav-count nav-text"
            >{{ conversations.unreadCount > 99 ? '99+' : conversations.unreadCount }}</span
          >
        </RouterLink>
      </section>
    </nav>
  </aside>
</template>
