export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
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
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  dueAt: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}
