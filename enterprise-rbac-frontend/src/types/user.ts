import type { PageQuery } from './common'

/**
 * 用户查询 DTO
 */
export interface UserQueryDTO extends PageQuery {
  username?: string
  status?: 0 | 1
}

/**
 * 用户 VO
 */
export interface UserVO {
  id: number
  username: string
  nickname?: string
  email?: string
  phone?: string
  status: 0 | 1
  createTime: string
  updateTime: string
  roles?: Array<{
    id: number
    roleCode: string
    roleName: string
  }>
}

/**
 * 创建用户 DTO
 */
export interface UserCreateDTO {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
  status: 0 | 1
  roleIds?: number[]
}

/**
 * 更新用户 DTO
 */
export interface UserUpdateDTO {
  nickname?: string
  email?: string
  phone?: string
  status: 0 | 1
}

/**
 * 修改密码 DTO
 */
export interface ChangePasswordDTO {
  oldPassword: string
  newPassword: string
}

/**
 * 重置密码 DTO
 */
export interface ResetPasswordDTO {
  newPassword: string
}

/**
 * 分配角色 DTO
 */
export interface AssignRolesDTO {
  roleIds: number[]
}
