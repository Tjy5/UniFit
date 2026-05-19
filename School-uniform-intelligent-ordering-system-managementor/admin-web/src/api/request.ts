import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/auth'

export interface ApiResponse<T> {
  code: number
  msg: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/admin-api',
  timeout: 15000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (typeof body?.code === 'number' && body.code !== 200) {
      ElMessage.error(body.msg || 'Request failed')
      if (body.code === 401) {
        clearToken()
        if (window.location.pathname !== '/login') {
          window.location.replace('/login')
        }
      }
      return Promise.reject(new Error(body.msg || 'Request failed'))
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.msg || error.message || 'Network error'
    if (error.response?.status === 401) {
      clearToken()
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default request
