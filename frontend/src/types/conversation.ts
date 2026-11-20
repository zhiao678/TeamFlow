export type ConversationType = 'DIRECT' | 'TEAM'
export type ConversationStatus = 'ACTIVE' | 'READ_ONLY'
export interface Conversation {
  id: number
  type: ConversationType
  recruitmentId: number | null
  status: ConversationStatus
  createdAt: string
  otherUserId: number | null
  otherUserDisplayName: string | null
  lastMessage: string | null
  lastMessageAt: string | null
  unreadCount: number
}
export interface ConversationMessage {
  id: number
  conversationId: number
  senderId: number
  senderDisplayName: string
  clientMessageId: string
  sourceRecruitmentId: number | null
  content: string
  status: string
  createdAt: string
}
export interface MessageQuery {
  page?: number
  pageSize?: number
  afterMessageId?: number
}
export interface MessagePage {
  records: ConversationMessage[]
  page: number
  pageSize: number
  total: number
  hasMore: boolean
  unreadCount: number
}
export interface SendMessageInput {
  clientMessageId: string
  content: string
  sourceRecruitmentId?: number
}
export interface ConversationRead {
  conversationId: number
  lastReadMessageId: number | null
  unreadCount: number
}
