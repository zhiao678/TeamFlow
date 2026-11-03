<script setup lang="ts">
import { computed, ref } from 'vue'
import { useWorkspace } from '@/composables/useWorkspace'
import type { TaskStatus } from '@/types/task'
const { tasks, loading, error, load, projectName } = useWorkspace()
const status = ref<TaskStatus | 'ALL'>('ALL')
const labels = { TODO: '待处理', IN_PROGRESS: '进行中', DONE: '已完成' }
const filtered = computed(() =>
  tasks.value.filter((task) => status.value === 'ALL' || task.status === status.value),
)
</script>
<template>
  <div class="page-heading">
    <div>
      <h1>我的任务</h1>
      <p>临时汇总当前可见项目中的任务，仅显示负责人为当前登录用户的任务。</p>
    </div>
    <el-button :loading="loading" @click="load">刷新</el-button>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <h2>任务列表</h2>
      <el-select v-model="status" class="status-select" aria-label="任务状态"
        ><el-option label="全部状态" value="ALL" /><el-option
          v-for="(label, value) in labels"
          :key="value"
          :label="label"
          :value="value"
      /></el-select>
    </div>
    <p v-if="loading" class="profile-panel" role="status">正在加载…</p>
    <div v-else-if="error" class="profile-panel">
      <p role="alert">{{ error }}</p>
      <el-button @click="load">重新加载</el-button>
    </div>
    <el-table v-else :data="filtered" empty-text="当前条件下暂无任务" class="tasks-table"
      ><el-table-column label="任务" min-width="260"
        ><template #default="{ row }"
          ><RouterLink :to="`/projects/${row.projectId}/tasks`">{{
            row.title
          }}</RouterLink></template
        ></el-table-column
      ><el-table-column label="所属项目" min-width="190"
        ><template #default="{ row }">{{ projectName(row.projectId) }}</template></el-table-column
      ><el-table-column label="状态" width="120"
        ><template #default="{ row }">{{
          labels[row.status as TaskStatus]
        }}</template></el-table-column
      ><el-table-column label="截止日期" width="140"
        ><template #default="{ row }">{{
          row.dueAt?.slice(0, 10) || '未设置'
        }}</template></el-table-column
      ></el-table
    >
  </section>
</template>
