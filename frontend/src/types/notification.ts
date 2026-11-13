export interface Notification {
  id: number
  // 后端可能新增事件类型，保留原始字符串并提供通用展示。
  eventType: string
  targetType: string
  recruitmentId: number | null
  applicationId: number | null
  leaderTransferId: number | null
  title: string
  content: string
  createdAt: string
  read: boolean
}
export interface NotificationQuery {
  page: number
  pageSize: number
  unreadOnly: boolean
}
export interface NotificationPage {
  records: Notification[]
  page: number
  pageSize: number
  total: number
  unreadCount: number
}
