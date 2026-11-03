<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkspace } from '@/composables/useWorkspace'
const auth = useAuthStore()
const { projects, tasks, loading, error, load, projectName } = useWorkspace()
const pending = computed(() => tasks.value.filter((task) => task.status !== 'DONE'))
const stats = computed(() => [
  { label: '可见项目', value: projects.value.length, to: '/projects' },
  {
    label: '我的待处理任务',
    value: tasks.value.filter((task) => task.status === 'TODO').length,
    to: '/tasks',
  },
  {
    label: '我的进行中任务',
    value: tasks.value.filter((task) => task.status === 'IN_PROGRESS').length,
    to: '/tasks',
  },
])
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>你好，{{ auth.user?.displayName }}</h1>
      <p>任务统计汇总自当前可见项目，按当前登录用户的负责人 ID 筛选。</p>
    </div>
    <el-button :loading="loading" @click="load">刷新</el-button>
  </div>
  <p v-if="loading" role="status">正在加载项目与任务…</p>
  <section v-else-if="error" class="panel profile-panel">
    <p role="alert">{{ error }}</p>
    <el-button @click="load">重新加载</el-button>
  </section>
  <template v-else>
    <section class="stats-grid">
      <RouterLink v-for="stat in stats" :key="stat.label" :to="stat.to" class="stat-card"
        ><div class="stat-top">{{ stat.label }}</div>
        <strong>{{ stat.value }}<small>项</small></strong></RouterLink
      >
      <div class="stat-card">
        <div class="stat-top">未读消息</div>
        <strong>—</strong>
        <p>消息接口暂未开放</p>
      </div>
    </section>
    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-heading">
          <h2>我的待办</h2>
          <RouterLink to="/tasks">查看全部 →</RouterLink>
        </div>
        <el-empty v-if="!pending.length" description="当前可见项目下暂无待办" />
        <article v-for="task in pending" :key="task.id" class="task-row">
          <div class="row-main">
            <RouterLink :to="`/projects/${task.projectId}/tasks`">{{ task.title }}</RouterLink>
            <p>
              {{ projectName(task.projectId) }} · 截止 {{ task.dueAt?.slice(0, 10) || '未设置' }}
            </p>
          </div>
        </article>
      </section>
      <section class="panel">
        <div class="panel-heading"><h2>最新消息</h2></div>
        <el-empty description="消息接口暂未开放" />
      </section>
      <section class="panel projects-panel">
        <div class="panel-heading">
          <h2>最近项目</h2>
          <RouterLink to="/projects">查看全部 →</RouterLink>
        </div>
        <el-empty v-if="!projects.length" description="暂无项目" />
        <div v-else class="project-grid">
          <article v-for="project in projects.slice(0, 3)" :key="project.id" class="project-mini">
            <h3>
              <RouterLink :to="`/projects/${project.id}`">{{ project.name }}</RouterLink>
            </h3>
            <p>{{ project.description || '暂无描述' }}</p>
            <div class="project-footer">
              <span>{{ project.ownerDisplayName }} · 负责人</span
              ><el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'">{{
                project.status === 'ACTIVE' ? '进行中' : '已归档'
              }}</el-tag>
            </div>
          </article>
        </div>
      </section>
    </div>
  </template>
</template>
