import axios from 'axios'

// 保留完整 Axios 响应，业务响应格式待与后端约定。
export const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})
