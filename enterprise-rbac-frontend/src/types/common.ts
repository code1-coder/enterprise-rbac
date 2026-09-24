/**
 * 统一响应结构
 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/**
 * 分页响应结构
 */
export interface PageResult<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  page?: number
  size?: number
}
