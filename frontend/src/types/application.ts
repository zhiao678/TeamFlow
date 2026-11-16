export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN' | 'EXPIRED'

/** 申请对象；字段与后端 `ApplicationVO` 一致。 */
export interface Application {
  id: number
  recruitmentId: number
  recruitmentTitle: string
  applicantId: number
  applicantDisplayName: string
  introduction: string | null
  skillsDescription: string | null
  availableTime: string | null
  status: ApplicationStatus
  rejectionReason: string | null
  createdAt: string
  processedAt: string | null
}

export type MemberRole = 'LEADER' | 'MEMBER'

/**
 * 正式成员；`id` 是用户 ID，不是成员关系记录 ID。
 * 服务端已按负责人优先、其余按加入时间与用户 ID 升序排好，展示时保持原顺序。
 */
export interface TeamMember {
  id: number
  displayName: string
  avatarUrl: string | null
  role: MemberRole
  joinedAt: string
}

export const applicationStatusLabels: Record<ApplicationStatus, string> = {
  PENDING: '待处理',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  WITHDRAWN: '已撤回',
  EXPIRED: '已失效',
}

export const memberRoleLabels: Record<MemberRole, string> = {
  LEADER: '负责人',
  MEMBER: '成员',
}

/** 只有待处理、且招募仍在招募中时才允许接受或拒绝。 */
export function canReview(status: ApplicationStatus, recruitment: { status: string } | null) {
  return status === 'PENDING' && recruitment?.status === 'RECRUITING'
}

export interface ApplicationInput {
  introduction?: string
  skillsDescription?: string
  availableTime?: string
}
