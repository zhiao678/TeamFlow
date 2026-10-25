<script setup lang="ts">
import { computed, ref } from 'vue'
import { myTasks, projectName, taskStatusLabels, priorityLabels, formatDate } from '@/mocks/data'
import type { TaskStatus } from '@/types/task'
const status = ref<TaskStatus | 'ALL'>('ALL')
const filteredTasks = computed(() =>
  myTasks.filter((task) => status.value === 'ALL' || task.status === status.value),
)
</script>
<template>
  <div class="page-heading">
    <div>
      <p class="eyebrow">专注每一件重要的事</p>
      <h1>我的任务</h1>
      <p>清晰安排优先级，让每一步都有进展。</p>
    </div>
  </div>
  <section class="panel">
    <div class="panel-heading">
      <h2>
        任务列表 <span class="count-label">{{ filteredTasks.length }}</span>
      </h2>
      <el-select v-model="status" aria-label="按任务状态筛选" class="status-select"
        ><el-option label="全部状态" value="ALL" /><el-option
          v-for="(label, value) in taskStatusLabels"
          :key="value"
          :label="label"
          :value="value"
      /></el-select>
    </div>
    <el-table :data="filteredTasks" empty-text="当前状态下暂无任务" class="tasks-table"
      ><el-table-column label="任务名称" prop="title" min-width="260" /><el-table-column
        label="所属项目"
        min-width="190"
        ><template #default="{ row }">{{ projectName(row.projectId) }}</template></el-table-column
      ><el-table-column label="优先级" width="110"
        ><template #default="{ row }"
          ><el-tag :type="row.priority === 'HIGH' ? 'danger' : 'info'" effect="plain">{{
            priorityLabels[row.priority as keyof typeof priorityLabels]
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="状态" width="120"
        ><template #default="{ row }"
          ><el-tag
            :type="
              row.status === 'DONE' ? 'success' : row.status === 'IN_PROGRESS' ? 'warning' : 'info'
            "
            >{{ taskStatusLabels[row.status as TaskStatus] }}</el-tag
          ></template
        ></el-table-column
      ><el-table-column label="截止日期" width="140"
        ><template #default="{ row }">{{ formatDate(row.dueAt) }}</template></el-table-column
      ></el-table
    >
  </section>
</template>
