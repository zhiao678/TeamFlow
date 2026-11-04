<script setup lang="ts">
import { displayDate, displayStatus, type Recruitment } from '@/types/recruitment'
defineProps<{ item: Recruitment; now: number }>()
</script>
<template>
  <article class="panel recruit-card">
    <div class="recruit-meta">
      <span>{{ item.categoryName }}</span
      ><strong>{{ displayStatus(item, now) }}</strong>
    </div>
    <h2>
      <RouterLink
        :to="{ name: 'recruitment-detail', params: { id: item.id }, query: $route.query }"
        >{{ item.title }}</RouterLink
      >
    </h2>
    <p>{{ item.courseName || item.purpose }}</p>
    <p>负责人：{{ item.ownerDisplayName }}</p>
    <div class="recruit-tags">
      <span v-for="tag in item.tags" :key="tag">{{ tag }}</span
      ><small v-if="!item.tags.length">未指定技能</small>
    </div>
    <div class="recruit-meta">
      <b>{{ item.currentMembers }} / {{ item.maxMembers }} 人 · 余 {{ item.remainingSlots }} 席</b
      ><small>截止：{{ displayDate(item.deadlineAt) }}</small>
    </div>
  </article>
</template>
