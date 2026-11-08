export interface UserProfile {
  id: number
  username: string
  displayName: string
  avatarUrl: string | null
  department: string | null
  grade: string | null
  bio: string | null
  platformRole: 'USER' | 'ADMIN'
  accountStatus: 'ACTIVE' | 'RESTRICTED' | 'DISABLED'
  skills: string[]
}
export interface Credentials {
  username: string
  password: string
}
export interface RegisterPayload extends Credentials {
  displayName: string
}
export type ProfilePayload = Pick<UserProfile, 'displayName'> & {
  avatarUrl: string
  department: string
  grade: string
  bio: string
  tagIds?: number[]
}
export interface AuthResult {
  token: string
  user: UserProfile
}
