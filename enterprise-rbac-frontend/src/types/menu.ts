/**
 * 菜单类型：M-目录 C-菜单 F-按钮 A-接口
 */
export type MenuType = 'M' | 'C' | 'F' | 'A'

/**
 * 菜单树节点 VO
 */
export interface MenuTreeVO {
  id: number
  parentId: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort: number
  visible: 0 | 1
  status: 0 | 1
  children?: MenuTreeVO[]
}

/**
 * 菜单 VO
 */
export interface MenuVO {
  id: number
  parentId: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort: number
  visible: 0 | 1
  status: 0 | 1
  remark?: string
  createTime: string
  updateTime: string
}

/**
 * 创建菜单 DTO
 */
export interface MenuCreateDTO {
  parentId: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort?: number
  visible?: 0 | 1
  status?: 0 | 1
  remark?: string
}

/**
 * 更新菜单 DTO
 */
export interface MenuUpdateDTO {
  parentId: number
  menuName: string
  menuType: MenuType
  path?: string
  component?: string
  permission?: string
  icon?: string
  sort?: number
  visible?: 0 | 1
  status?: 0 | 1
  remark?: string
}
