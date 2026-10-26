<script setup lang="ts">
import type { Project } from '@/types/project'
defineProps<{ project: Project }>()
</script>
<template>
  <section class="panel project-section">
    <h2>项目概览</h2>
    <p class="project-description">{{ project.description || '暂无项目描述' }}</p>
    <dl class="project-facts">
      <div>
        <dt>创建者</dt>
        <dd>{{ project.ownerDisplayName }}</dd>
      </div>
      <div>
        <dt>项目状态</dt>
        <dd>{{ project.status === 'ACTIVE' ? '进行中' : '已归档' }}</dd>
      </div>
      <div>
        <dt>创建时间</dt>
        <dd>{{ project.createdAt.replace('T', ' ') }}</dd>
      </div>
      <div>
        <dt>最近更新</dt>
        <dd>{{ project.updatedAt.replace('T', ' ') }}</dd>
      </div>
    </dl>
  </section>
</template>
