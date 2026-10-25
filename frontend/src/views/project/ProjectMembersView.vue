<script setup lang="ts">
import type { Project } from '@/types/project'
defineProps<{ project: Project }>()
</script>
<template>
  <section class="panel project-section">
    <h2>项目成员</h2>
    <p>当前仅展示项目详情中已提供的创建者信息，完整成员列表暂未开放。</p>
    <div class="project-owner">
      <span class="avatar">{{ project.ownerDisplayName.slice(0, 1) }}</span>
      <div>
        <strong>{{ project.ownerDisplayName }}</strong>
        <p>项目创建者</p>
      </div>
    </div>
    <el-empty description="完整成员列表暂不可用" :image-size="80" />
  </section>
</template>
