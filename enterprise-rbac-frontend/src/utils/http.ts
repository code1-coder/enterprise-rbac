import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import type { Result } from '@/types/common'

/**
 * 统一 HTTP 客户端
 * 负责请求头注入、响应拦截、错误处理和认证失效回调
 */
class HttpClient {
  private instance: AxiosInstance
  private authFailedCallback?: () => void

  constructor() {
    this.instance = axios.create({
      baseURL: '/api',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  /**
   * 设置认证失效回调（由路由守卫注册）
   */
  setAuthFailedCallback(callback: () => void) {
    this.authFailedCallback = callback
  }

  /**
   * 设置请求和响应拦截器
   */
  private setupInterceptors() {
    // 请求拦截器：注入 Authorization 头
    this.instance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const token = sessionStorage.getItem('token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // 响应拦截器：统一处理业务错误和认证失效
    this.instance.interceptors.response.use(
      (response: AxiosResponse<Result>) => {
        const { code, message } = response.data
        
        // 业务成功
        if (code === 200) {
          return response
        }
        
        // 业务错误，抛出异常由调用方处理
        return Promise.reject(new Error(message || '请求失败'))
      },
      (error) => {
        // HTTP 错误处理
        if (error.response) {
          const { status, data } = error.response
          
          if (status === 401) {
            // 认证失效，清理登录态并跳转登录页
            this.authFailedCallback?.()
            return Promise.reject(new Error('登录已过期，请重新登录'))
          }
          
          if (status === 403) {
            return Promise.reject(new Error('权限不足'))
          }
          
          if (status === 404) {
            return Promise.reject(new Error('资源不存在'))
          }
          
          // 其他错误，优先使用后端返回的 message
          const message = data?.message || '服务异常'
          return Promise.reject(new Error(message))
        }
        
        // 网络错误
        if (error.code === 'ECONNABORTED') {
          return Promise.reject(new Error('请求超时'))
        }
        
        return Promise.reject(new Error('网络连接失败'))
      }
    )
  }

  /**
   * GET 请求
   */
  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.get<Result<T>>(url, config)
    return response.data.data
  }

  /**
   * POST 请求
   */
  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.post<Result<T>>(url, data, config)
    return response.data.data
  }

  /**
   * PUT 请求
   */
  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.put<Result<T>>(url, data, config)
    return response.data.data
  }

  /**
   * DELETE 请求
   */
  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.instance.delete<Result<T>>(url, config)
    return response.data.data
  }
}

// 导出单例
export const http = new HttpClient()
