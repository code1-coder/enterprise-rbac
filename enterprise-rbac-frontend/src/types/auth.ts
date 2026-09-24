/**
 * 登录请求
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 登录响应
 */
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

/**
 * 用户信息摘要
 */
export interface UserInfo {
  id: number
  username: string
  nickname?: string
  roles: string[]
  permissions: string[]
}

/**
 * 注册请求
 */
export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
}
