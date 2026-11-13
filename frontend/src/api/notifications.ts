import axios from 'axios'
import { request } from './request'
import type { NotificationPage, NotificationQuery } from '@/types/notification'

interface Result<T> {
  code: number
  message: string
  data: T
}
class NotificationError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}
async function unwrap<T>(call: Promise<{ data: Result<T> }>) {
  const { data } = await call
  if (data.code !== 0) throw new NotificationError(data.code, data.message)
  return data.data
}
export const listNotifications = (params: NotificationQuery, signal?: AbortSignal) =>
  unwrap(request.get<Result<NotificationPage>>('/notifications', { params, signal }))
export const markNotificationRead = (id: number) =>
  unwrap(request.patch<Result<null>>(`/notifications/${id}/read`))
export const markAllNotificationsRead = () =>
  unwrap(request.patch<Result<null>>('/notifications/read-all'))
export function notificationError(cause: unknown) {
  const status =
    cause instanceof NotificationError
      ? Math.floor(cause.code / 100)
      : axios.isAxiosError(cause)
        ? cause.response?.status
        : undefined
  switch (status) {
    case 400:
      return '通知请求参数无效，请重试。'
    case 401:
      return '登录已失效，请重新登录。'
    case 403:
      return '你没有访问这些通知的权限。'
    case 404:
      return '通知不存在或不属于当前账号，请刷新列表。'
    case 409:
      return '通知状态已变化，请刷新后重试。'
    case 500:
      return '通知服务暂时出错，请稍后重试。'
    default:
      return '通知请求失败，请检查网络后重试。'
  }
}
