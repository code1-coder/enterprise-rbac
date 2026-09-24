import { http } from '@/utils/http'
import type { PageResult } from '@/types/common'
import type {
  UserQueryDTO,
  UserVO,
  UserCreateDTO,
  UserUpdateDTO,
  ChangePasswordDTO,
  ResetPasswordDTO,
  AssignRolesDTO
} from '@/types/user'

/**
 * 用户管理 API
 */
export const usersApi = {
  /**
   * 分页查询用户列表
   */
  getList(params: UserQueryDTO): Promise<PageResult<UserVO>> {
    return http.get('/users', { params })
  },

  /**
   * 获取用户详情
   */
  getById(id: number): Promise<UserVO> {
    return http.get(`/users/${id}`)
  },

  /**
   * 创建用户
   */
  create(data: UserCreateDTO): Promise<void> {
    return http.post('/users', data)
  },

  /**
   * 更新用户
   */
  update(id: number, data: UserUpdateDTO): Promise<void> {
    return http.put(`/users/${id}`, data)
  },

  /**
   * 删除用户
   */
  delete(id: number): Promise<void> {
    return http.delete(`/users/${id}`)
  },

  /**
   * 批量删除用户
   */
  batchDelete(ids: number[]): Promise<void> {
    return http.delete('/users/batch', { data: { ids } })
  },

  /**
   * 修改自己的密码
   */
  changePassword(id: number, data: ChangePasswordDTO): Promise<void> {
    return http.put(`/users/${id}/password`, data)
  },

  /**
   * 管理员重置密码
   */
  resetPassword(id: number, data: ResetPasswordDTO): Promise<void> {
    return http.put(`/users/${id}/reset-password`, data)
  },

  /**
   * 分配用户角色
   */
  assignRoles(id: number, data: AssignRolesDTO): Promise<void> {
    return http.put(`/users/${id}/roles`, data)
  }
}
