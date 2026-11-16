import type { Notification } from '@/types/notification'

export interface NotificationTarget {
  path: string | null
  label: string
  hint: string
  query?: Record<string, string>
}
const validId = (id: number | null): id is number =>
  id !== null && Number.isSafeInteger(id) && id > 0
// 只允许已有业务路由，不使用通知文本作为 URL。申请通知当前只有 applicationId。
export function notificationTarget(
  item: Notification,
  availablePaths: readonly string[],
): NotificationTarget {
  if (item.targetType === 'APPLICATION' && validId(item.applicationId)) {
    if (['APPLICATION_SUBMITTED', 'APPLICATION_WITHDRAWN'].includes(item.eventType)) {
      return availablePaths.includes('/my-recruitments')
        ? {
            path: '/my-recruitments',
            label: '查看我的招募',
            hint: '请从对应招募进入申请管理，当前通知未提供招募编号。',
          }
        : { path: null, label: '', hint: '招募管理入口尚未接入，可稍后查看。' }
    }
    if (
      ['APPLICATION_ACCEPTED', 'APPLICATION_REJECTED', 'APPLICATION_EXPIRED'].includes(
        item.eventType,
      )
    ) {
      return availablePaths.includes('/my-applications')
        ? {
            path: '/my-applications',
            query: { applicationId: String(item.applicationId) },
            label: '查看我的申请',
            hint: '',
          }
        : { path: null, label: '', hint: '我的申请页面尚未接入，请稍后从该页面查看处理结果。' }
    }
  }
  if (
    item.targetType === 'RECRUITMENT' &&
    validId(item.recruitmentId) &&
    availablePaths.includes('/recruitments/:id')
  ) {
    return { path: `/recruitments/${item.recruitmentId}`, label: '查看招募', hint: '' }
  }
  return { path: null, label: '', hint: '此通知暂不支持直接跳转，请查看通知内容。' }
}
