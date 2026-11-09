<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
async function logout() {
  if (await auth.logout()) await router.replace('/login')
}
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
        >工作空间 <span>/</span><strong>{{ route.meta.title }}</strong></span
      >
    </div>
    <div class="header-end">
      <RouterLink to="/notifications" class="message-link">消息</RouterLink
      ><template v-if="auth.user"
        ><RouterLink to="/profile" class="message-link"
          ><span class="avatar">{{ auth.user.displayName.slice(0, 1) || '用户' }}</span
          >{{ auth.user.displayName }}</RouterLink
        ><el-button text :loading="auth.loggingOut" @click="logout">退出登录</el-button></template
      ><template v-else-if="auth.token"
        ><span class="message-link">{{ auth.loading ? '正在验证身份…' : '身份待验证' }}</span
        ><el-button text :loading="auth.loggingOut" @click="logout">退出登录</el-button></template
      ><RouterLink v-else to="/login">登录</RouterLink>
    </div>
  </header>
</template>
