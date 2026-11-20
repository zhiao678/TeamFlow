export type MemberExitStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'
export type LeaderTransferStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'
export interface MemberExitRequest {
  id: number
  recruitmentId: number
  recruitmentTitle: string
  userId: number
  userDisplayName: string
  status: MemberExitStatus
  reason: string | null
  rejectionReason: string | null
  createdAt: string
  processedAt: string | null
  handoverHint: string
}
export interface LeaderTransfer {
  id: number
  recruitmentId: number
  recruitmentTitle: string
  fromUserId: number
  fromDisplayName: string
  toUserId: number
  toDisplayName: string
  status: LeaderTransferStatus
  exitAfterTransfer: boolean
  createdAt: string
  processedAt: string | null
  handoverHint: string
}
export const memberExitLabels: Record<MemberExitStatus, string> = {
  PENDING: '待审批',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  EXPIRED: '已失效',
}
export const leaderTransferLabels: Record<LeaderTransferStatus, string> = {
  PENDING: '待确认',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  EXPIRED: '已失效',
}
