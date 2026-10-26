<script setup lang="ts">
import type { Project } from '@/types/project'
defineProps<{ project: Project }>()
</script>
<template>
  <section class="panel project-section">
    <h2>{{ project.name }}的任务</h2>
    <el-empty description="项目任务功能正在建设中" />
    <p>任务列表上线后，可在此查看和管理当前项目的任务。</p>
  </section>
</template>
