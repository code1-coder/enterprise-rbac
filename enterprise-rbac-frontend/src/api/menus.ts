import { http } from '@/utils/http'
import type { MenuTreeVO, MenuVO, MenuCreateDTO, MenuUpdateDTO } from '@/types/menu'

/**
 * 菜单权限管理 API
 */
export const menusApi = {
  /**
   * 获取菜单树（管理端）
   */
  getTree(): Promise<MenuTreeVO[]> {
    return http.get('/menus/tree')
  },

  /**
   * 获取当前用户的导航菜单
   */
  getMyMenus(): Promise<MenuTreeVO[]> {
    return http.get('/menus/my-menus')
  },

  /**
   * 获取当前用户的按钮权限标识
   */
  getMyPermissions(): Promise<string[]> {
    return http.get('/menus/my-permissions')
  },

  /**
   * 获取菜单详情
   */
  getById(id: number): Promise<MenuVO> {
    return http.get(`/menus/${id}`)
  },

  /**
   * 创建菜单
   */
  create(data: MenuCreateDTO): Promise<void> {
    return http.post('/menus', data)
  },

  /**
   * 更新菜单
   */
  update(id: number, data: MenuUpdateDTO): Promise<void> {
    return http.put(`/menus/${id}`, data)
  },

  /**
   * 删除菜单
   */
  delete(id: number): Promise<void> {
    return http.delete(`/menus/${id}`)
  }
}
