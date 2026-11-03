<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const collapsed = ref(false)
</script>

<template>
  <div class="app-layout" :class="{ 'is-collapsed': collapsed }">
    <AppSidebar :collapsed="collapsed" />
    <div class="workspace">
      <AppHeader :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <main class="page-content">
        <div v-if="auth.error" class="panel session-feedback" role="alert">
          <p>暂时无法确认登录身份：{{ auth.error }}。凭据已保留，请重试。</p>
          <el-button :loading="auth.loading" @click="auth.restore(true)">重新验证</el-button>
        </div>
        <RouterView />
      </main>
      <footer class="app-footer">TeamFlow · 让团队协作井然有序</footer>
    </div>
  </div>
</template>
