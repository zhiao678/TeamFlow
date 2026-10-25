<script setup lang="ts">
import { projects, formatDate } from '@/mocks/data'
</script>
<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">一起向目标前进</p>
      <h1>项目</h1>
      <p>查看团队正在推进与已经完成的项目。</p>
    </div>
    <span class="team-label">共 {{ projects.length }} 个项目</span>
  </div>
  <div class="project-grid">
    <article v-for="project in projects" :key="project.id" class="panel project-card">
      <div class="project-card-top">
        <span class="project-icon">▣</span
        ><el-tag :type="project.status === 'ACTIVE' ? 'success' : 'info'">{{
          project.status === 'ACTIVE' ? '进行中' : '已归档'
        }}</el-tag>
      </div>
      <h2>{{ project.name }}</h2>
      <p>{{ project.description }}</p>
      <div class="project-footer">
        <span>负责人 · {{ project.ownerDisplayName }}</span
        ><span>{{ formatDate(project.updatedAt) }}</span>
      </div>
    </article>
  </div>
</template>
