import { ref } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken, setSessionToken } from '@/api/session'
import { getCurrentUser, updateProfile, logout as logoutRequest, errorMessage } from '@/api/users'
import type { AuthResult, UserProfile, ProfilePayload } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const error = ref('')
  const loading = ref(false)
  const loggingOut = ref(false)
  let revision = 0
  let pending: Promise<void> | null = null
  function clear() {
    revision++
    pending = null
    loading.value = false
    setSessionToken('')
    user.value = null
    error.value = ''
  }
  function accept(result: AuthResult) {
    clear()
    setSessionToken(result.token)
    user.value = result.user
    error.value = ''
  }
  async function restore(force = false) {
    if (!sessionToken.value || (user.value && !force)) return
    if (pending) return pending
    const token = sessionToken.value
    const currentRevision = revision
    loading.value = true
    error.value = ''
    pending = (async () => {
      try {
        const profile = await getCurrentUser()
        if (sessionToken.value === token && revision === currentRevision) {
          user.value = profile
          error.value = ''
        }
      } catch (cause) {
        if (sessionToken.value === token && revision === currentRevision)
          error.value = errorMessage(cause)
      }
    })().finally(() => {
      if (revision === currentRevision) {
        pending = null
        loading.value = false
      }
    })
    return pending
  }
  async function saveProfile(payload: ProfilePayload) {
    const currentRevision = revision
    const token = sessionToken.value
    const profile = await updateProfile(payload)
    if (currentRevision !== revision || token !== sessionToken.value) return false
    user.value = profile
    return true
  }
  async function logout() {
    if (loggingOut.value) return false
    loggingOut.value = true
    const currentRevision = revision
    const token = sessionToken.value
    try {
      await logoutRequest()
    } catch {
      /* 无状态 JWT：请求失败也结束本地会话。 */
    } finally {
      if (revision === currentRevision && sessionToken.value === token) clear()
      loggingOut.value = false
    }
    return !sessionToken.value
  }
  return {
    loggingOut,
    logout,
    token: sessionToken,
    user,
    error,
    loading,
    clear,
    accept,
    restore,
    saveProfile,
  }
})
