import { request } from './request'
import { unwrapTeamResult, type TeamResult } from './team-result'
import type { Recruitment } from '@/types/recruitment'
import type { LeaderTransfer, MemberExitRequest } from '@/types/team-management'

export const formTeam = (recruitmentId: number) =>
  unwrapTeamResult(
    request.post<TeamResult<Recruitment>>(`/recruitments/${recruitmentId}/form-team`),
  )
export const removeTeamMember = (recruitmentId: number, userId: number) =>
  unwrapTeamResult(
    request.delete<TeamResult<null>>(`/recruitments/${recruitmentId}/members/${userId}`),
  )
export const requestMemberExit = (recruitmentId: number, reason?: string) =>
  unwrapTeamResult(
    request.post<TeamResult<MemberExitRequest>>(`/recruitments/${recruitmentId}/exit-requests`, {
      reason,
    }),
  )
export const listRecruitmentExits = (recruitmentId: number, signal?: AbortSignal) =>
  unwrapTeamResult(
    request.get<TeamResult<MemberExitRequest[]>>(`/recruitments/${recruitmentId}/exit-requests`, {
      signal,
    }),
  )
export const listMyMemberExits = (signal?: AbortSignal) =>
  unwrapTeamResult(
    request.get<TeamResult<MemberExitRequest[]>>('/member-exit-requests/me', { signal }),
  )
export const approveMemberExit = (id: number) =>
  unwrapTeamResult(
    request.patch<TeamResult<MemberExitRequest>>(`/member-exit-requests/${id}/approve`),
  )
export const rejectMemberExit = (id: number, reason?: string) =>
  unwrapTeamResult(
    request.patch<TeamResult<MemberExitRequest>>(`/member-exit-requests/${id}/reject`, { reason }),
  )
export const requestLeadershipExit = (recruitmentId: number, successorId: number) =>
  unwrapTeamResult(
    request.post<TeamResult<LeaderTransfer>>(
      `/recruitments/${recruitmentId}/leader-exit-requests`,
      { successorId },
    ),
  )
export const listMyLeaderTransfers = (signal?: AbortSignal) =>
  unwrapTeamResult(request.get<TeamResult<LeaderTransfer[]>>('/leader-transfers/me', { signal }))
export const acceptLeaderTransfer = (id: number) =>
  unwrapTeamResult(request.patch<TeamResult<LeaderTransfer>>(`/leader-transfers/${id}/accept`))
export const rejectLeaderTransfer = (id: number) =>
  unwrapTeamResult(request.patch<TeamResult<LeaderTransfer>>(`/leader-transfers/${id}/reject`))
