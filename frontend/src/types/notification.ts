// 前端展示用临时类型，待消息接口确定后对齐。
export interface Notification {
  id: number
  title: string
  content: string
  createdAt: string
  read: boolean
  target: '/projects' | '/tasks'
}
