<script setup lang="ts">
import { computed } from 'vue'
import {
  currentUser,
  projects,
  myTasks,
  projectName,
  taskStatusLabels,
  priorityLabels,
  formatDate,
} from '@/mocks/data'
import { useNotificationsStore } from '@/stores/notifications'
const notifications = useNotificationsStore()
const pendingTasks = myTasks.filter((task) => task.status !== 'DONE')
const stats = computed(() => [
  {
    label: '参与项目',
    value: projects.length,
    hint: '共同推进每个目标',
    to: '/projects',
    tone: 'blue',
  },
  {
    label: '待处理任务',
    value: myTasks.filter((task) => task.status === 'TODO').length,
    hint: '准备好，开始下一步',
    to: '/tasks',
    tone: 'amber',
  },
  {
    label: '进行中任务',
    value: myTasks.filter((task) => task.status === 'IN_PROGRESS').length,
    hint: '专注当下，持续推进',
    to: '/tasks',
    tone: 'green',
  },
  {
    label: '未读消息',
    value: notifications.unreadCount,
    hint: '不错过团队的最新动态',
    to: '/notifications',
    tone: 'purple',
  },
])
</script>

<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">你的团队工作空间</p>
      <h1>你好，{{ currentUser.name }} <span class="greeting-dot">✦</span></h1>
      <p>从这里开始，开启有条不紊的一天。</p>
    </div>
    <span class="team-label">{{ currentUser.team }}</span>
  </div>
  <section class="stats-grid" aria-label="工作统计">
    <RouterLink v-for="stat in stats" :key="stat.label" :to="stat.to" class="stat-card"
      ><div class="stat-top">
        <span>{{ stat.label }}</span
        ><span class="stat-symbol" :class="stat.tone">↗</span>
      </div>
      <strong>{{ stat.value }}<small>项</small></strong>
      <p>{{ stat.hint }}</p></RouterLink
    >
  </section>
  <div class="dashboard-grid">
    <section class="panel">
      <div class="panel-heading">
        <h2>
          我的待办 <span class="count-label">{{ pendingTasks.length }}</span>
        </h2>
        <RouterLink to="/tasks">查看全部 →</RouterLink>
      </div>
      <div class="task-list">
        <article v-for="task in pendingTasks" :key="task.id" class="task-row">
          <span class="task-marker" :class="task.status === 'IN_PROGRESS' ? 'active' : ''"></span>
          <div class="row-main">
            <RouterLink to="/tasks" class="item-title">{{ task.title }}</RouterLink>
            <p>{{ projectName(task.projectId) }} · 截止 {{ formatDate(task.dueAt) }}</p>
          </div>
          <el-tag :type="task.priority === 'HIGH' ? 'danger' : 'info'" effect="plain" size="small"
            >{{ priorityLabels[task.priority] }}优先级</el-tag
          ><el-tag :type="task.status === 'IN_PROGRESS' ? 'warning' : 'info'" size="small">{{
            taskStatusLabels[task.status]
          }}</el-tag>
        </article>
      </div>
    </section>
    <section class="panel">
      <div class="panel-heading">
        <h2>最新消息</h2>
        <RouterLink to="/notifications">全部消息 →</RouterLink>
      </div>
      <article v-for="item in notifications.items.slice(0, 3)" :key="item.id" class="activity-row">
        <span class="activity-dot" :class="{ read: item.read }"></span>
        <div>
          <RouterLink to="/notifications" class="item-title">{{ item.title }}</RouterLink>
          <p>{{ item.content }}</p>
          <time>{{ item.createdAt.replace('T', ' ').slice(0, 16) }}</time>
        </div>
      </article>
    </section>
    <section class="panel projects-panel">
      <div class="panel-heading">
        <h2>最近项目</h2>
        <RouterLink to="/projects">查看全部 →</RouterLink>
      </div>
      <div class="project-grid">
        <article v-for="project in projects" :key="project.id" class="project-mini">
          <span class="project-icon">▣</span>
          <h3>
            <RouterLink to="/projects">{{ project.name }}</RouterLink>
          </h3>
          <p>{{ project.description }}</p>
          <div class="project-footer">
            <span>{{ project.ownerDisplayName }} · 负责人</span
            ><el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{
              project.status === 'ACTIVE' ? '进行中' : '已归档'
            }}</el-tag>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
