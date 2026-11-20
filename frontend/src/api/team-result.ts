import axios from 'axios'
export interface TeamResult<T> {
  code: number
  message: string
  data: T
}
export class TeamRequestError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message)
  }
}
export async function unwrapTeamResult<T>(call: Promise<{ data: TeamResult<T> }>): Promise<T> {
  const { data } = await call
  if (data.code !== 0) throw new TeamRequestError(data.code, data.message || '请求失败')
  return data.data
}
export function teamErrorStatus(cause: unknown) {
  if (cause instanceof Error && 'code' in cause && typeof cause.code === 'number')
    return Math.floor(cause.code / 100)
  return axios.isAxiosError(cause) ? cause.response?.status : undefined
}
export function teamErrorMessage(cause: unknown) {
  switch (teamErrorStatus(cause)) {
    case 400:
      return '请求内容不符合要求，请检查字段和参数。'
    case 401:
      return '登录已失效，请重新登录。'
    case 403:
      return '当前账号没有操作权限，成员或会话资格可能已变化，请刷新。'
    case 404:
      return '队伍、会话、用户或记录不存在，请刷新后确认。'
    case 409:
      return '状态已变化或重复操作，请重新加载实际结果，不要自动重复提交。'
    case 500:
      return '服务端处理失败，请稍后重试。'
    default:
      return '请求失败，请检查网络后重试。'
  }
}
