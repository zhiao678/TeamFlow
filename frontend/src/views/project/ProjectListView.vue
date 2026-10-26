<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getProjects, projectError } from '@/api/projects'
import type { Project } from '@/types/project'
const projects = ref<Project[]>([])
const loading = ref(true)
const error = ref('')
let controller: AbortController | undefined
async function load() {
  controller?.abort()
  const current = new AbortController()
  controller = current
  loading.value = true
  error.value = ''
  try {
    projects.value = await getProjects(current.signal)
  } catch (cause) {
    if (!current.signal.aborted) error.value = projectError(cause).message
  } finally {
    if (!current.signal.aborted) loading.value = false
  }
}
onMounted(load)
onBeforeUnmount(() => controller?.abort())
</script>
<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">团队项目</p>
      <h1>项目</h1>
      <p>查看团队项目，进入详情了解项目进展。</p>
    </div>
    <el-button :loading="loading" @click="load">刷新列表</el-button>
  </div>
  <section v-if="loading" class="panel project-section" role="status" aria-label="正在加载项目">
    <el-skeleton :rows="5" animated />
    <p>正在加载项目…</p>
  </section>
  <section v-else-if="error" class="panel project-section" role="alert">
    <h2>项目加载失败</h2>
    <p>{{ error }}</p>
    <el-button type="primary" @click="load">重新加载</el-button>
  </section>
  <section v-else-if="!projects.length" class="panel"><el-empty description="暂无项目" /></section>
  <template v-else>
    <p class="project-count">共 {{ projects.length }} 个项目</p>
    <div class="project-grid">
      <RouterLink
        v-for="project in projects"
        :key="project.id"
        :to="{ name: 'project-overview', params: { projectId: project.id } }"
        class="panel project-card project-card-link"
      >
        <div class="project-card-top">
          <span class="project-icon">▣</span
          ><el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'">{{
            project.status === 'ACTIVE' ? '进行中' : '已归档'
          }}</el-tag>
        </div>
        <h2>{{ project.name }}</h2>
        <p>{{ project.description || '暂无项目描述' }}</p>
        <div class="project-footer">
          <span>创建者 · {{ project.ownerDisplayName }}</span
          ><span>{{ project.updatedAt.slice(0, 10) }}</span>
        </div>
        <span class="project-enter">查看项目 →</span>
      </RouterLink>
    </div>
  </template>
</template>
