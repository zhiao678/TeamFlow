<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getProject, projectError } from '@/api/projects'
import type { Project } from '@/types/project'
const route = useRoute()
const project = ref<Project | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const retry = ref(0)
const tabs = [
  { name: 'project-overview', label: '概览' },
  { name: 'project-members', label: '成员' },
  { name: 'project-tasks', label: '任务' },
]
watch(
  [() => route.params.projectId, retry],
  async ([rawId], _, onCleanup) => {
    const controller = new AbortController()
    onCleanup(() => controller.abort())
    project.value = null
    error.value = ''
    notFound.value = false
    loading.value = true
    const id = Number(rawId)
    if (!/^\d+$/.test(String(rawId)) || !Number.isSafeInteger(id) || id <= 0) {
      error.value = '项目编号无效，请返回项目列表重新选择'
      notFound.value = true
      loading.value = false
      return
    }
    try {
      project.value = await getProject(id, controller.signal)
    } catch (cause) {
      if (!controller.signal.aborted) {
        const failure = projectError(cause)
        error.value = failure.message
        notFound.value = failure.notFound
      }
    } finally {
      if (!controller.signal.aborted) loading.value = false
    }
  },
  { immediate: true },
)
</script>
<template>
  <RouterLink to="/projects" class="project-back">← 返回项目列表</RouterLink>
  <section v-if="loading" class="panel project-section" role="status" aria-label="正在加载项目详情">
    <el-skeleton :rows="6" animated />
    <p>正在加载项目详情…</p>
  </section>
  <section v-else-if="error" class="panel project-section" role="alert">
    <h1>{{ notFound ? '项目不存在' : '项目加载失败' }}</h1>
    <p>{{ error }}</p>
    <el-button v-if="!notFound" type="primary" @click="retry++">重新加载</el-button>
  </section>
  <template v-else-if="project">
    <div class="page-heading">
      <div>
        <p class="eyebrow">项目详情</p>
        <h1>{{ project.name }}</h1>
        <p>创建者 · {{ project.ownerDisplayName }}</p>
      </div>
      <el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'">{{
        project.status === 'ACTIVE' ? '进行中' : '已归档'
      }}</el-tag>
    </div>
    <nav class="project-tabs" aria-label="项目导航">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.name"
        :to="{ name: tab.name, params: { projectId: project.id } }"
        >{{ tab.label }}</RouterLink
      >
    </nav>
    <RouterView v-slot="{ Component }"
      ><component :is="Component" :key="project.id" :project="project"
    /></RouterView>
  </template>
</template>
