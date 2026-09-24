import type { PageQuery } from './common'

/**
 * 角色查询 DTO
 */
export interface RoleQueryDTO extends PageQuery {
  roleName?: string
}

/**
 * 角色 VO
 */
export interface RoleVO {
  id: number
  roleName: string
  roleCode: string
  status: 0 | 1
  sort: number
  remark?: string
  createTime: string
  updateTime: string
}

/**
 * 角色选项（下拉框）
 */
export interface RoleOption {
  id: number
  roleName: string
  roleCode: string
}

/**
 * 创建角色 DTO
 */
export interface RoleCreateDTO {
  roleName: string
  roleCode: string
  status: 0 | 1
  sort?: number
  remark?: string
}

/**
 * 更新角色 DTO
 */
export interface RoleUpdateDTO {
  roleName: string
  roleCode: string
  status: 0 | 1
  sort?: number
  remark?: string
}

/**
 * 角色权限 DTO
 */
export interface RolePermissionsDTO {
  menuIds: number[]
}
