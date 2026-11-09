import axios from 'axios'
import { sessionToken } from './session'

// 保留完整 Axios 响应，业务响应格式待与后端约定。
export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  if (sessionToken.value && !['/auth/login', '/auth/register'].includes(config.url ?? '')) {
    config.headers.Authorization = `Bearer ${sessionToken.value}`
  }
  return config
})
request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.headers.Authorization === `Bearer ${sessionToken.value}` &&
      sessionToken.value
    ) {
      window.dispatchEvent(new Event('teamflow:unauthorized'))
    }
    return Promise.reject(error)
  },
)
