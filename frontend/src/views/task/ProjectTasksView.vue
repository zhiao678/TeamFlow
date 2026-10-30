<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createProjectTask, getProjectTasks, taskError, updateTaskStatus } from '@/api/tasks'
import type { Project } from '@/types/project'
import type { Task, TaskPriority, TaskStatus } from '@/types/task'

const props = defineProps<{ project: Project }>()

const taskStatusLabels: Record<TaskStatus, string> = {
  TODO: '待处理',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
}
const priorityLabels: Record<TaskPriority, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }

const statuses = Object.keys(taskStatusLabels) as TaskStatus[]

const tasks = ref<Task[]>([])
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const retry = ref(0)
const viewMode = ref<'list' | 'board'>('list')
const statusFilter = ref<TaskStatus | 'ALL'>('ALL')
const statusUpdatingId = ref<number | null>(null)

const detailTask = ref<Task | null>(null)
const createVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  title: '',
  description: '',
  assigneeId: null as number | null,
  priority: 'MEDIUM' as TaskPriority,
  dueAt: null as string | null,
})
const rules = {
  title: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { max: 200, message: '任务名称不能超过 200 个字符', trigger: 'blur' },
  ],
}

const filteredTasks = computed(() =>
  tasks.value.filter((task) => statusFilter.value === 'ALL' || task.status === statusFilter.value),
)

const kanbanColumns = computed(() =>
  statuses.map((status) => ({
    status,
    label: taskStatusLabels[status],
    tasks: filteredTasks.value.filter((task) => task.status === status),
  })),
)

// 后端暂无成员列表接口，先用项目创建者与已有任务的负责人、创建者汇总成员选项。
const memberOptions = computed(() => {
  const members = new Map<number, string>()
  members.set(props.project.ownerId, props.project.ownerDisplayName)
  for (const task of tasks.value) {
    if (task.assigneeId && task.assigneeDisplayName) {
      members.set(task.assigneeId, task.assigneeDisplayName)
    }
    if (task.creatorId && task.creatorDisplayName) {
      members.set(task.creatorId, task.creatorDisplayName)
    }
  }
  return [...members].map(([id, name]) => ({ id, name }))
})

watch(
  [() => props.project.id, retry],
  async (_, __, onCleanup) => {
    const controller = new AbortController()
    onCleanup(() => controller.abort())
    tasks.value = []
    error.value = ''
    notFound.value = false
    loading.value = true
    try {
      tasks.value = await getProjectTasks(props.project.id, controller.signal)
    } catch (cause) {
      if (!controller.signal.aborted) {
        const failure = taskError(cause, '无法加载任务列表，请检查网络及后端服务后重试')
        error.value = failure.message
        notFound.value = failure.notFound
      }
    } finally {
      if (!controller.signal.aborted) loading.value = false
    }
  },
  { immediate: true },
)

function replaceTask(updated: Task) {
  const index = tasks.value.findIndex((task) => task.id === updated.id)
  if (index >= 0) {
    tasks.value.splice(index, 1, updated)
  } else {
    tasks.value.push(updated)
  }
}

async function changeStatus(task: Task, next: TaskStatus) {
  if (task.status === next || statusUpdatingId.value !== null) return
  statusUpdatingId.value = task.id
  try {
    const updated = await updateTaskStatus(task.id, next)
    // 成功后才用后端返回的任务更新界面；失败时保留原状态并提示。
    replaceTask(updated)
    if (detailTask.value?.id === task.id) detailTask.value = updated
    ElMessage.success('任务状态已更新')
  } catch (cause) {
    ElMessage.error(taskError(cause, '修改任务状态失败，请稍后重试').message)
  } finally {
    statusUpdatingId.value = null
  }
}

function openCreate() {
  createVisible.value = true
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.assigneeId = null
  form.priority = 'MEDIUM'
  form.dueAt = null
  formRef.value?.clearValidate()
}

async function submitCreate() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createProjectTask(props.project.id, {
      title: form.title.trim(),
      description: form.description.trim() || undefined,
      // 临时方案：暂无登录态，默认以项目创建者身份提交创建请求。
      creatorId: props.project.ownerId,
      assigneeId: form.assigneeId ?? undefined,
      priority: form.priority,
      dueAt: form.dueAt ?? undefined,
    })
    ElMessage.success('任务创建成功')
    createVisible.value = false
    resetForm()
    await loadTasksQuietly()
  } catch (cause) {
    ElMessage.error(taskError(cause, '创建任务失败，请稍后重试').message)
  } finally {
    submitting.value = false
  }
}

async function loadTasksQuietly() {
  try {
    tasks.value = await getProjectTasks(props.project.id)
  } catch {
    ElMessage.error('创建成功，但刷新任务列表失败，请手动刷新页面')
  }
}

function statusTagType(status: TaskStatus) {
  return status === 'DONE' ? 'success' : status === 'IN_PROGRESS' ? 'warning' : 'info'
}

function priorityTagType(priority: TaskPriority) {
  return priority === 'HIGH' ? 'danger' : priority === 'MEDIUM' ? 'warning' : 'info'
}

function formatDate(value: string | null) {
  return value ? value.slice(0, 10) : '未设置'
}

function formatDateTime(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '未设置'
}
</script>

<template>
  <section class="panel project-section task-panel">
    <div class="panel-heading">
      <h2>
        任务列表 <span class="count-label">{{ filteredTasks.length }}</span>
      </h2>
      <div class="task-toolbar">
        <el-select v-model="statusFilter" aria-label="按任务状态筛选" class="task-status-select">
          <el-option label="全部状态" value="ALL" />
          <el-option
            v-for="(label, value) in taskStatusLabels"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
        <el-radio-group v-model="viewMode" aria-label="切换视图">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="board">看板</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="openCreate">新建任务</el-button>
      </div>
    </div>

    <div v-if="loading" class="task-body" role="status" aria-label="正在加载任务列表">
      <el-skeleton :rows="5" animated />
      <p>正在加载任务列表…</p>
    </div>

    <div v-else-if="error" class="task-body" role="alert">
      <h3>{{ notFound ? '项目不存在' : '任务列表加载失败' }}</h3>
      <p>{{ error }}</p>
      <el-button v-if="!notFound" type="primary" @click="retry++">重新加载</el-button>
    </div>

    <template v-else>
      <div v-if="filteredTasks.length === 0" class="task-body">
        <el-empty
          :description="statusFilter === 'ALL' ? '当前项目还没有任务' : '当前状态下暂无任务'"
        >
          <el-button v-if="statusFilter === 'ALL'" type="primary" @click="openCreate"
            >新建任务</el-button
          >
        </el-empty>
      </div>

      <el-table v-else-if="viewMode === 'list'" :data="filteredTasks" class="tasks-table">
        <el-table-column label="任务名称" min-width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="detailTask = row">{{ row.title }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="plain">{{
              priorityLabels[row.priority as TaskPriority]
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="130">
          <template #default="{ row }">{{ row.assigneeDisplayName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-select
              :model-value="row.status"
              :disabled="statusUpdatingId !== null"
              aria-label="修改任务状态"
              @change="(next: TaskStatus) => changeStatus(row, next)"
            >
              <el-option
                v-for="(label, value) in taskStatusLabels"
                :key="value"
                :label="label"
                :value="value"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="130">
          <template #default="{ row }">{{ formatDate(row.dueAt) }}</template>
        </el-table-column>
      </el-table>

      <div v-else class="kanban">
        <section v-for="column in kanbanColumns" :key="column.status" class="kanban-column">
          <header class="kanban-column-head">
            <h3>{{ column.label }}</h3>
            <span class="count-label">{{ column.tasks.length }}</span>
          </header>
          <div class="kanban-cards">
            <article
              v-for="task in column.tasks"
              :key="task.id"
              class="kanban-card"
              @click="detailTask = task"
            >
              <p class="kanban-card-title">{{ task.title }}</p>
              <div class="kanban-card-meta">
                <el-tag size="small" :type="priorityTagType(task.priority)" effect="plain">
                  {{ priorityLabels[task.priority] }}
                </el-tag>
                <span class="kanban-assignee">{{ task.assigneeDisplayName || '未分配' }}</span>
              </div>
              <div class="kanban-card-foot">
                <span class="kanban-due">截止 {{ formatDate(task.dueAt) }}</span>
                <el-dropdown
                  trigger="click"
                  @command="(next: TaskStatus) => changeStatus(task, next)"
                >
                  <el-button
                    size="small"
                    text
                    type="primary"
                    :disabled="statusUpdatingId !== null"
                    @click.stop
                    >移动到</el-button
                  >
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="(label, value) in taskStatusLabels"
                        :key="value"
                        :command="value"
                        :disabled="value === task.status"
                        >{{ label }}</el-dropdown-item
                      >
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </article>
            <p v-if="column.tasks.length === 0" class="kanban-empty">暂无任务</p>
          </div>
        </section>
      </div>
    </template>
  </section>

  <el-dialog
    :model-value="detailTask !== null"
    title="任务详情"
    width="560px"
    @update:model-value="detailTask = null"
  >
    <el-descriptions v-if="detailTask" :column="1" border>
      <el-descriptions-item label="任务名称">{{ detailTask.title }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailTask.status)">{{
          taskStatusLabels[detailTask.status]
        }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="优先级">
        <el-tag :type="priorityTagType(detailTask.priority)" effect="plain">{{
          priorityLabels[detailTask.priority]
        }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="负责人">{{
        detailTask.assigneeDisplayName || '未分配'
      }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{
        detailTask.creatorDisplayName
      }}</el-descriptions-item>
      <el-descriptions-item label="截止时间">{{
        formatDateTime(detailTask.dueAt)
      }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{
        formatDateTime(detailTask.completedAt)
      }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{
        formatDateTime(detailTask.createdAt)
      }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{
        formatDateTime(detailTask.updatedAt)
      }}</el-descriptions-item>
      <el-descriptions-item label="任务描述">{{
        detailTask.description || '暂无描述'
      }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <el-dialog v-model="createVisible" title="新建任务" width="520px" @closed="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="84px" @submit.prevent>
      <el-form-item label="任务名称" prop="title">
        <el-input
          v-model="form.title"
          maxlength="200"
          show-word-limit
          placeholder="请输入任务名称"
        />
      </el-form-item>
      <el-form-item label="任务描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="补充任务背景或验收标准（可选）"
        />
      </el-form-item>
      <el-form-item label="负责人" prop="assigneeId">
        <el-select
          v-model="form.assigneeId"
          clearable
          placeholder="选择负责人（可选）"
          class="task-form-select"
        >
          <el-option
            v-for="member in memberOptions"
            :key="member.id"
            :label="member.name"
            :value="member.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-select v-model="form.priority" class="task-form-select">
          <el-option
            v-for="(label, value) in priorityLabels"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="截止时间" prop="dueAt">
        <el-date-picker
          v-model="form.dueAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="选择截止时间（可选）"
          class="task-form-select"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitCreate">创建任务</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.task-panel {
  overflow: visible;
}
.task-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.task-status-select {
  width: 132px;
}
.task-body {
  padding: 26px 24px;
}
.task-body h3 {
  margin-bottom: 8px;
}
.task-body .el-button {
  margin-top: 14px;
}
.task-form-select {
  width: 100%;
}
.kanban {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 20px;
  background: #f8faff;
}
.kanban-column {
  background: #fff;
  border: 1px solid #e8ecf3;
  border-radius: 10px;
  min-height: 220px;
  display: flex;
  flex-direction: column;
}
.kanban-column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f6;
}
.kanban-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  flex: 1;
}
.kanban-card {
  border: 1px solid #e8ecf3;
  border-radius: 9px;
  padding: 13px 14px;
  cursor: pointer;
  transition: box-shadow 0.15s;
}
.kanban-card:hover {
  box-shadow: 0 5px 18px #20304b0b;
}
.kanban-card-title {
  color: #33425c;
  font-size: 13px;
  font-weight: 500;
}
.kanban-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}
.kanban-assignee {
  font-size: 12px;
  color: #78849a;
}
.kanban-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.kanban-due {
  font-size: 11px;
  color: #8b95a7;
}
.kanban-empty {
  text-align: center;
  font-size: 12px;
  color: #9aa5b7;
  padding: 20px 0;
}
@media (max-width: 980px) {
  .kanban {
    grid-template-columns: 1fr;
  }
}
</style>
