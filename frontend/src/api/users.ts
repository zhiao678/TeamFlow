import axios from 'axios'
import { request } from './request'
import type {
  AuthResult,
  Credentials,
  RegisterPayload,
  UserProfile,
  ProfilePayload,
} from '@/types/user'
interface Result<T> {
  code: number
  message: string
  data: T
}
async function unwrap<T>(response: Promise<{ data: Result<T> }>) {
  const { data } = await response
  if (data.code !== 0) throw new Error(data.message || '请求失败')
  return data.data
}
export const login = (payload: Credentials) =>
  unwrap(request.post<Result<AuthResult>>('/auth/login', payload))
export const register = (payload: RegisterPayload) =>
  unwrap(request.post<Result<AuthResult>>('/auth/register', payload))
export const getCurrentUser = () => unwrap(request.get<Result<UserProfile>>('/users/me'))
export const updateProfile = (payload: ProfilePayload) =>
  unwrap(request.patch<Result<UserProfile>>('/users/me', payload))
export function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message
    return typeof message === 'string' ? message : '请求失败，请检查网络和后端服务后重试'
  }
  return error instanceof Error ? error.message : '请求失败，请重试'
}
