import { ref, onMounted, onUnmounted } from 'vue'
import { getProjects } from '@/api/projects'
import { getProjectTasks } from '@/api/tasks'
import { errorMessage } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import type { Project } from '@/types/project'
import type { Task } from '@/types/task'

// 临时汇总当前项目列表下的任务，后端提供个人任务接口后替换。
export function useWorkspace() {
  const auth = useAuthStore()
  const projects = ref<Project[]>([])
  const tasks = ref<Task[]>([])
  const loading = ref(false)
  const error = ref('')
  let controller: AbortController | undefined
  async function load() {
    controller?.abort()
    const current = new AbortController()
    controller = current
    loading.value = true
    error.value = ''
    projects.value = []
    tasks.value = []
    try {
      await auth.restore()
      if (!auth.user) throw new Error(auth.error || '请先登录后加载工作空间')
      const userId = auth.user.id
      const list = await getProjects(current.signal)
      const all: Task[] = []
      // 顺序请求避免项目较多时同时发出大量请求。
      for (const project of list) all.push(...(await getProjectTasks(project.id, current.signal)))
      if (current.signal.aborted || auth.user?.id !== userId) return
      projects.value = list
      tasks.value = all.filter((task) => task.assigneeId === userId)
    } catch (cause) {
      if (!current.signal.aborted) error.value = errorMessage(cause)
    } finally {
      if (!current.signal.aborted) loading.value = false
    }
  }
  function projectName(id: number) {
    return projects.value.find((project) => project.id === id)?.name ?? '未知项目'
  }
  onMounted(load)
  onUnmounted(() => controller?.abort())
  return { projects, tasks, loading, error, load, projectName }
}
