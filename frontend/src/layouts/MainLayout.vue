<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const narrowScreen = window.matchMedia('(max-width: 760px)')
const narrow = ref(narrowScreen.matches)
const desktopCollapsed = ref(false)
const mobileOpen = ref(false)
const collapsed = computed(() => (narrow.value ? !mobileOpen.value : desktopCollapsed.value))

function updateScreen(event: MediaQueryListEvent) {
  narrow.value = event.matches
  mobileOpen.value = false
}

function toggleSidebar() {
  if (narrow.value) mobileOpen.value = !mobileOpen.value
  else desktopCollapsed.value = !desktopCollapsed.value
}

onMounted(() => narrowScreen.addEventListener('change', updateScreen))
onUnmounted(() => narrowScreen.removeEventListener('change', updateScreen))
</script>

<template>
  <div class="app-layout" :class="{ 'is-collapsed': collapsed }">
    <AppHeader :collapsed="collapsed" @toggle="toggleSidebar" />
    <AppSidebar :collapsed="collapsed" @navigate="mobileOpen = false" />
    <main class="page-content">
      <div v-if="auth.error" class="panel session-feedback" role="alert">
        <p>暂时无法确认登录身份：{{ auth.error }}。凭据已保留，请重试。</p>
        <el-button :loading="auth.loading" @click="auth.restore(true)">重新验证</el-button>
      </div>
      <RouterView />
    </main>
    <footer class="app-footer">TeamFlow · 让团队协作井然有序</footer>
  </div>
</template>
