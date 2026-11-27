import { request } from './request'
import { unwrapTeamResult, type TeamResult } from './team-result'
import type {
  Conversation,
  ConversationMessage,
  ConversationRead,
  MessagePage,
  MessageQuery,
  SendMessageInput,
} from '@/types/conversation'

export const listConversations = (signal?: AbortSignal) =>
  unwrapTeamResult(request.get<TeamResult<Conversation[]>>('/conversations', { signal }))
export const createPrivateConversation = (targetUserId: number) =>
  unwrapTeamResult(
    request.post<TeamResult<Conversation>>('/conversations/private', { targetUserId }),
  )
export const getTeamConversation = (recruitmentId: number, signal?: AbortSignal) =>
  unwrapTeamResult(
    request.get<TeamResult<Conversation>>(`/recruitments/${recruitmentId}/team-conversation`, {
      signal,
    }),
  )
export const listMessages = (
  conversationId: number,
  params: MessageQuery = {},
  signal?: AbortSignal,
) =>
  unwrapTeamResult(
    request.get<TeamResult<MessagePage>>(`/conversations/${conversationId}/messages`, {
      params,
      signal,
    }),
  )
// clientMessageId 由调用方创建并保存，失败重试原样复用；这里不生成新 ID。
export const sendMessage = (conversationId: number, input: SendMessageInput) =>
  unwrapTeamResult(
    request.post<TeamResult<ConversationMessage>>(
      `/conversations/${conversationId}/messages`,
      input,
    ),
  )
export const markConversationRead = (conversationId: number, lastReadMessageId: number) =>
  unwrapTeamResult(
    request.patch<TeamResult<ConversationRead>>(`/conversations/${conversationId}/read`, {
      lastReadMessageId,
    }),
  )
