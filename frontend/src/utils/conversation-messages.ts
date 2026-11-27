import type { ConversationMessage, SendMessageInput } from '@/types/conversation'

/** 创建一次后保存此对象，重试时不能重新调用本方法或改变原消息正文。 */
export function createMessageDraft(
  content: string,
  sourceRecruitmentId?: number,
): SendMessageInput {
  if (!content.trim() || content.length > 2000) throw new Error('消息必须为 1–2000 字符的非空文本')
  if (
    sourceRecruitmentId !== undefined &&
    (!Number.isSafeInteger(sourceRecruitmentId) || sourceRecruitmentId <= 0)
  )
    throw new Error('来源招募编号必须为正整数')
  return {
    clientMessageId: crypto.randomUUID(),
    content,
    ...(sourceRecruitmentId === undefined ? {} : { sourceRecruitmentId }),
  }
}
/** 历史、轮询和发送结果共用；只合并同一会话，按服务端 ID 去重、升序排列。 */
export function mergeConversationMessages(
  conversationId: number,
  current: readonly ConversationMessage[],
  incoming: readonly ConversationMessage[],
): ConversationMessage[] {
  const rows = [...current, ...incoming]
  if (rows.some((row) => row.conversationId !== conversationId))
    throw new Error('不能合并不同会话的消息')
  return [...new Map(rows.map((row) => [row.id, row])).values()].sort((a, b) => a.id - b.id)
}
