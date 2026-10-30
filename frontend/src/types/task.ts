export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export interface Task {
  id: number
  projectId: number
  creatorId: number
  creatorDisplayName: string
  assigneeId: number | null
  assigneeDisplayName: string | null
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}
