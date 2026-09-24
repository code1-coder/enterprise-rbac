import { http } from '@/utils/http'
import type { LoginRequest, LoginResponse, RegisterRequest, UserInfo } from '@/types/auth'

/**
 * 认证相关 API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login(data: LoginRequest): Promise<LoginResponse> {
    return http.post('/auth/login', data)
  },

  /**
   * 用户注册
   */
  register(data: RegisterRequest): Promise<void> {
    return http.post('/auth/register', data)
  },

  /**
   * 退出登录
   */
  logout(): Promise<void> {
    return http.post('/auth/logout')
  },

  /**
   * 刷新 Token
   */
  refreshToken(): Promise<LoginResponse> {
    return http.post('/auth/refresh-token')
  },

  /**
   * 获取当前用户信息
   */
  getUserInfo(): Promise<UserInfo> {
    return http.get('/auth/user-info')
  }
}
