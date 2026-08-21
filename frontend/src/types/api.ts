/**
 * 统一 API 响应类型
 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 分页响应
 */
export interface PageResult<T> {
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
  current?: number
  size?: number
  keyword?: string
  orderBy?: string
  orderDir?: 'ASC' | 'DESC'
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  deptId?: number
  deptName?: string
  roles: Array<{ id: number; roleName: string; roleCode: string }>
  permissions: string[]
}

/**
 * 登录响应
 */
export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
}