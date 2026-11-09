<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
defineProps<{ collapsed: boolean }>()
const links = [
  { to: '/recruitments', label: '招募大厅', icon: '◎' },
  { to: '/my-recruitments', label: '我的招募', icon: '✎' },
  { to: '/dashboard', label: '工作台', icon: '◫' },
  { to: '/projects', label: '项目', icon: '▣' },
  { to: '/tasks', label: '我的任务', icon: '☑' },
  { to: '/notifications', label: '消息', icon: '♧' },
  { to: '/profile', label: '个人资料', icon: '◉' },
]
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <RouterLink to="/dashboard" class="brand" aria-label="TeamFlow 工作台"
      ><span class="brand-mark">▦</span><span class="nav-text">TeamFlow</span></RouterLink
    >
    <p class="nav-caption nav-text">团队协作空间</p>
    <nav aria-label="主导航">
      <RouterLink
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        :title="link.label"
        class="nav-item"
        :class="{
          'router-link-active':
            ['/projects', '/recruitments'].includes(link.to) &&
            route.path.startsWith(link.to + '/'),
        }"
      >
        <span class="nav-icon" aria-hidden="true">{{ link.icon }}</span
        ><span class="nav-text">{{ link.label }}</span>
      </RouterLink>
    </nav>
    <div class="sidebar-note nav-text">
      <strong>一起，把事情做好。</strong>
      <p>每一步进展，都值得被看见。</p>
    </div>
  </aside>
</template>
