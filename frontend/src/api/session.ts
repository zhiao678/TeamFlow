import { ref } from 'vue'
const key = 'teamflow.token'
function readToken() {
  try {
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}
export const sessionToken = ref(readToken())
export function setSessionToken(value: string) {
  sessionToken.value = value
  try {
    if (value) localStorage.setItem(key, value)
    else localStorage.removeItem(key)
  } catch {
    /* 当前会话仍可使用，浏览器禁用存储时不持久化。 */
  }
}
