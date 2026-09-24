import { http } from '@/utils/http'
import type { PageResult } from '@/types/common'
import type {
  RoleQueryDTO,
  RoleVO,
  RoleOption,
  RoleCreateDTO,
  RoleUpdateDTO,
  RolePermissionsDTO
} from '@/types/role'

/**
 * 角色管理 API
 */
export const rolesApi = {
  /**
   * 分页查询角色列表
   */
  getList(params: RoleQueryDTO): Promise<PageResult<RoleVO>> {
    return http.get('/roles', { params })
  },

  /**
   * 获取所有角色（下拉框）
   */
  getAllRoles(): Promise<RoleOption[]> {
    return http.get('/roles/list')
  },

  /**
   * 获取角色详情
   */
  getById(id: number): Promise<RoleVO> {
    return http.get(`/roles/${id}`)
  },

  /**
   * 创建角色
   */
  create(data: RoleCreateDTO): Promise<void> {
    return http.post('/roles', data)
  },

  /**
   * 更新角色
   */
  update(id: number, data: RoleUpdateDTO): Promise<void> {
    return http.put(`/roles/${id}`, data)
  },

  /**
   * 删除角色
   */
  delete(id: number): Promise<void> {
    return http.delete(`/roles/${id}`)
  },

  /**
   * 获取角色已分配的菜单 ID
   */
  getPermissions(id: number): Promise<{ menuIds: number[] }> {
    return http.get(`/roles/${id}/permissions`)
  },

  /**
   * 分配角色菜单权限
   */
  assignPermissions(id: number, data: RolePermissionsDTO): Promise<void> {
    return http.put(`/roles/${id}/permissions`, data)
  }
}
