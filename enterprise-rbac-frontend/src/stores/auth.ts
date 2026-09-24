import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { storage } from '@/utils/storage'
import type { LoginRequest, UserInfo } from '@/types/auth'

/**
 * 认证状态 Store
 * 管理登录态、Token 和当前用户信息
 */
export const useAuthStore = defineStore('auth', () => {
  // 当前用户信息
  const userInfo = ref<UserInfo | null>(null)
  
  // 是否已登录
  const isAuthenticated = ref(false)

  /**
   * 登录
   */
  async function login(loginRequest: LoginRequest) {
    const response = await authApi.login(loginRequest)
    
    // 保存 Token
    storage.setToken(response.token, response.tokenType)
    
    // 保存用户信息
    userInfo.value = response.userInfo
    storage.setUserInfo(response.userInfo)
    
    isAuthenticated.value = true
    
    return response
  }

  /**
   * 退出登录
   */
  async function logout() {
    try {
      // 调用后端退出接口（无论成功与否都清理本地状态）
      await authApi.logout()
    } catch (error) {
      console.error('Logout API failed:', error)
    } finally {
      // 清理本地状态
      clearAuthState()
    }
  }

  /**
   * 刷新 Token
   */
  async function refreshToken() {
    const response = await authApi.refreshToken()
    
    // 替换为新 Token
    storage.setToken(response.token, response.tokenType)
    
    return response
  }

  /**
   * 获取当前用户信息
   */
  async function fetchUserInfo() {
    const info = await authApi.getUserInfo()
    userInfo.value = info
    storage.setUserInfo(info)
    isAuthenticated.value = true
    return info
  }

  /**
   * 从本地恢复登录态
   */
  function restoreAuth() {
    const token = storage.getToken()
    const savedUserInfo = storage.getUserInfo()
    
    if (token && savedUserInfo) {
      userInfo.value = savedUserInfo
      isAuthenticated.value = true
      return true
    }
    
    return false
  }

  /**
   * 清理认证状态（认证失效时调用）
   */
  function clearAuthState() {
    userInfo.value = null
    isAuthenticated.value = false
    storage.clearAuth()
  }

  /**
   * 检查是否有指定权限
   */
  function hasPermission(permission: string): boolean {
    return userInfo.value?.permissions?.includes(permission) ?? false
  }

  /**
   * 检查是否有任一权限
   */
  function hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(p => hasPermission(p))
  }

  return {
    userInfo,
    isAuthenticated,
    login,
    logout,
    refreshToken,
    fetchUserInfo,
    restoreAuth,
    clearAuthState,
    hasPermission,
    hasAnyPermission
  }
})
