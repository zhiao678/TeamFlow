import type { Project } from '@/types/project'
import type { Task, TaskStatus } from '@/types/task'
import type { Notification } from '@/types/notification'

export const currentUser = { id: 1, name: '张同学', team: 'TeamFlow 研发团队' }
const timestamp = '2026-10-25T09:00:00'
export const projects: Project[] = [
  {
    id: 1,
    name: '中级实训协同平台',
    description: '让项目推进更清晰，让团队协作更简单。',
    ownerId: 2,
    ownerDisplayName: '李同学',
    status: 'ACTIVE',
    createdAt: timestamp,
    updatedAt: timestamp,
  },
  {
    id: 2,
    name: '团队知识库',
    description: '沉淀开发规范、接口文档与项目经验。',
    ownerId: 1,
    ownerDisplayName: '张同学',
    status: 'ACTIVE',
    createdAt: timestamp,
    updatedAt: timestamp,
  },
  {
    id: 3,
    name: '产品官网改版',
    description: '完成品牌展示与产品介绍页面的第一轮迭代。',
    ownerId: 3,
    ownerDisplayName: '王同学',
    status: 'ARCHIVED',
    createdAt: timestamp,
    updatedAt: timestamp,
  },
]
export const tasks: Task[] = [
  {
    id: 1,
    projectId: 1,
    title: '完成整体布局与工作台页面',
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueAt: '2026-10-28T18:00:00',
  },
  {
    id: 2,
    projectId: 1,
    title: '对齐项目接口字段与状态枚举',
    status: 'TODO',
    priority: 'HIGH',
    dueAt: '2026-10-29T18:00:00',
  },
  {
    id: 3,
    projectId: 2,
    title: '整理前端协作与启动指引',
    status: 'TODO',
    priority: 'MEDIUM',
    dueAt: '2026-10-30T18:00:00',
  },
  {
    id: 4,
    projectId: 1,
    title: '初始化工程与代码检查配置',
    status: 'DONE',
    priority: 'MEDIUM',
    dueAt: '2026-10-24T18:00:00',
  },
  {
    id: 5,
    projectId: 3,
    title: '归档官网页面设计资料',
    status: 'DONE',
    priority: 'LOW',
    dueAt: null,
  },
].map((task) => ({
  ...task,
  status: task.status as TaskStatus,
  priority: task.priority as Task['priority'],
  creatorId: 2,
  creatorDisplayName: '李同学',
  assigneeId: 1,
  assigneeDisplayName: '张同学',
  description: null,
  completedAt: task.status === 'DONE' ? timestamp : null,
  createdAt: timestamp,
  updatedAt: timestamp,
}))

export const notifications: Notification[] = [
  {
    id: 1,
    title: '你有一项新的任务',
    content: '李同学将「对齐项目接口字段与状态枚举」分配给了你。',
    createdAt: '2026-10-25T10:30:00',
    read: false,
    target: '/tasks',
  },
  {
    id: 2,
    title: '项目协作提醒',
    content: '「中级实训协同平台」已更新接口规范，请团队成员查阅。',
    createdAt: '2026-10-25T09:20:00',
    read: false,
    target: '/projects',
  },
  {
    id: 3,
    title: '欢迎加入团队知识库',
    content: '在这里共同沉淀文档，让每一次协作都有迹可循。',
    createdAt: '2026-10-24T16:00:00',
    read: true,
    target: '/projects',
  },
]
export const myTasks = tasks.filter((task) => task.assigneeId === currentUser.id)
export const taskStatusLabels = { TODO: '待处理', IN_PROGRESS: '进行中', DONE: '已完成' }
export const priorityLabels = { LOW: '低', MEDIUM: '中', HIGH: '高' }
export function projectName(id: number) {
  return projects.find((project) => project.id === id)?.name ?? '未知项目'
}
export function formatDate(value: string | null) {
  return value ? value.slice(0, 10) : '未设置'
}
