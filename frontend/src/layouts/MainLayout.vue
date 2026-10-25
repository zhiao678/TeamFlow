<script setup lang="ts">
import { ref } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
const collapsed = ref(false)
</script>

<template>
  <div class="app-layout" :class="{ 'is-collapsed': collapsed }">
    <AppSidebar :collapsed="collapsed" />
    <div class="workspace">
      <AppHeader :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <main class="page-content"><RouterView /></main>
      <footer class="app-footer">TeamFlow · 让团队协作井然有序</footer>
    </div>
  </div>
</template>
