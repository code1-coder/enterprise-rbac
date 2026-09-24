/**
 * 统一的本地存储工具
 * 使用 sessionStorage 保存登录态，支持同标签页刷新恢复
 */

const TOKEN_KEY = 'token'
const TOKEN_TYPE_KEY = 'tokenType'
const USER_INFO_KEY = 'userInfo'

export const storage = {
  /**
   * 保存 Token
   */
  setToken(token: string, tokenType = 'Bearer') {
    sessionStorage.setItem(TOKEN_KEY, token)
    sessionStorage.setItem(TOKEN_TYPE_KEY, tokenType)
  },

  /**
   * 获取 Token
   */
  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY)
  },

  /**
   * 获取 Token 类型
   */
  getTokenType(): string {
    return sessionStorage.getItem(TOKEN_TYPE_KEY) || 'Bearer'
  },

  /**
   * 保存用户信息
   */
  setUserInfo(userInfo: any) {
    sessionStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
  },

  /**
   * 获取用户信息
   */
  getUserInfo(): any | null {
    const userInfo = sessionStorage.getItem(USER_INFO_KEY)
    return userInfo ? JSON.parse(userInfo) : null
  },

  /**
   * 清除所有认证信息
   */
  clearAuth() {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TOKEN_TYPE_KEY)
    sessionStorage.removeItem(USER_INFO_KEY)
  }
}
