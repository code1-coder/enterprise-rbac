import { defineStore } from 'pinia'
import { ref } from 'vue'
import { menusApi } from '@/api/menus'
import type { MenuTreeVO } from '@/types/menu'

/**
 * 权限状态 Store
 * 管理用户菜单和按钮权限
 */
export const usePermissionStore = defineStore('permission', () => {
  // 当前用户导航菜单树
  const menus = ref<MenuTreeVO[]>([])
  
  // 当前用户按钮权限标识列表
  const permissions = ref<string[]>([])
  
  // 是否已加载
  const loaded = ref(false)

  /**
   * 加载当前用户菜单和权限
   */
  async function loadUserPermissions() {
    const [userMenus, userPermissions] = await Promise.all([
      menusApi.getMyMenus(),
      menusApi.getMyPermissions()
    ])
    
    menus.value = userMenus
    permissions.value = userPermissions
    loaded.value = true
  }

  /**
   * 清理权限数据
   */
  function clearPermissions() {
    menus.value = []
    permissions.value = []
    loaded.value = false
  }

  /**
   * 检查是否有指定权限
   */
  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  /**
   * 检查是否有任一权限
   */
  function hasAnyPermission(permissionList: string[]): boolean {
    return permissionList.some(p => hasPermission(p))
  }

  return {
    menus,
    permissions,
    loaded,
    loadUserPermissions,
    clearPermissions,
    hasPermission,
    hasAnyPermission
  }
})
