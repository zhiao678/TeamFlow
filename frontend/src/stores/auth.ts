import { ref } from 'vue'
import { defineStore } from 'pinia'
import { sessionToken, setSessionToken } from '@/api/session'
import { getCurrentUser, errorMessage } from '@/api/users'
import type { AuthResult, UserProfile } from '@/types/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const error = ref('')
  let pending: Promise<void> | null = null
  function clear() {
    setSessionToken('')
    user.value = null
    error.value = ''
  }
  function accept(result: AuthResult) {
    setSessionToken(result.token)
    user.value = result.user
    error.value = ''
  }
  async function restore(force = false) {
    if (!sessionToken.value || (user.value && !force)) return
    if (pending) return pending
    const token = sessionToken.value
    pending = (async () => {
      try {
        const profile = await getCurrentUser()
        if (sessionToken.value === token) {
          user.value = profile
          error.value = ''
        }
      } catch (cause) {
        if (sessionToken.value === token) error.value = errorMessage(cause)
      }
    })().finally(() => {
      pending = null
    })
    return pending
  }
  return { token: sessionToken, user, error, clear, accept, restore }
})
