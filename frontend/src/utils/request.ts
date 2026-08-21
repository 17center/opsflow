import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'

/** JWT 存储 key */
const TOKEN_KEY = 'opsflow_token'
/** 刷新令牌存储 key */
const REFRESH_TOKEN_KEY = 'opsflow_refresh_token'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000
})

// 请求拦截器：注入 Token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理业务码
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    // 非 200 业务码视为错误
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      // 401 未认证：跳转登录
      if (res.code === 401) {
        clearToken()
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.message))
    }
    return response
  },
  (error) => {
    const status = error.response?.status
    let msg = '网络异常，请稍后重试'
    if (status === 401) {
      msg = '登录已过期，请重新登录'
      clearToken()
      window.location.href = '/login'
    } else if (status === 403) {
      msg = '无权限访问'
    } else if (status === 500) {
      msg = '服务端内部错误'
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** 通用请求方法：返回 data 数据 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request<ApiResult<T>>(config).then((res) => res.data.data)
}

export function get<T>(url: string, params?: object): Promise<T> {
  return request<T>({ url, method: 'get', params })
}

export function post<T>(url: string, data?: object): Promise<T> {
  return request<T>({ url, method: 'post', data })
}

export function put<T>(url: string, data?: object): Promise<T> {
  return request<T>({ url, method: 'put', data })
}

export function del<T>(url: string, params?: object): Promise<T> {
  return request<T>({ url, method: 'delete', params })
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string, refreshToken: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export default service