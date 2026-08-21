import { get, post, put, del, request } from '@/utils/request'
import type { PageResult } from '@/types/api'

/** 用户 */
export interface UserItem {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  deptId?: number
  status: number
  loginIp?: string
  loginTime?: string
  createTime?: string
  remark?: string
}

export interface UserForm {
  username?: string
  password?: string
  nickname: string
  email?: string
  phone?: string
  deptId?: number
  status: number
  remark?: string
}

export function pageUsers(params: {
  current?: number
  size?: number
  username?: string
  nickname?: string
  status?: number
  deptId?: number
}) {
  return get<PageResult<UserItem>>('/system/users', params)
}

export function createUser(data: UserForm) {
  return post<null>('/system/users', data)
}

export function updateUser(id: number, data: UserForm) {
  return put<null>(`/system/users/${id}`, data)
}

export function deleteUser(id: number) {
  return del<null>(`/system/users/${id}`)
}

export function changeUserStatus(id: number, status: number) {
  return put<null>(`/system/users/${id}/status`, { status })
}

export function resetUserPassword(id: number, password: string) {
  return put<null>(`/system/users/${id}/password`, { password })
}

/** 角色 */
export interface RoleItem {
  id: number
  roleName: string
  roleCode: string
  sortOrder?: number
  status: number
  dataScope?: number
  createTime?: string
  remark?: string
  menuIds?: number[]
}

export interface RoleForm {
  roleName: string
  roleCode?: string
  sortOrder?: number
  dataScope?: number
  status: number
  remark?: string
}

export function pageRoles(params: {
  current?: number
  size?: number
  roleName?: string
  roleCode?: string
  status?: number
}) {
  return get<PageResult<RoleItem>>('/system/roles', params)
}

export function getRoleDetail(id: number) {
  return get<RoleItem>(`/system/roles/${id}`)
}

export function createRole(data: RoleForm) {
  return post<null>('/system/roles', data)
}

export function updateRole(id: number, data: RoleForm) {
  return put<null>(`/system/roles/${id}`, data)
}

export function deleteRole(id: number) {
  return del<null>(`/system/roles/${id}`)
}

export function changeRoleStatus(id: number, status: number) {
  return put<null>(`/system/roles/${id}/status`, { status })
}

export function assignRoleMenus(id: number, menuIds: number[]) {
  return put<null>(`/system/roles/${id}/menus`, { menuIds })
}

export function getRoleMenuTree() {
  return get<MenuTreeNode[]>('/system/roles/menus/tree')
}

/** 菜单 */
export interface MenuTreeNode {
  id: number
  menuName: string
  parentId: number
  menuType: number
  permission?: string
  sortOrder?: number
  children?: MenuTreeNode[]
}

/** 菜单树节点（管理端，含 path/component/icon 等） */
export interface MenuAdminNode extends MenuTreeNode {
  path?: string
  component?: string
  icon?: string
  visible: number
  status: number
  createTime?: string
  remark?: string
}

export interface MenuForm {
  menuName: string
  parentId: number
  menuType: number
  path?: string
  component?: string
  permission?: string
  icon?: string
  sortOrder?: number
  visible: number
  status: number
  remark?: string
}

export function getMenuTree(params?: { menuName?: string; status?: number }) {
  return get<MenuAdminNode[]>('/system/menus/tree', params)
}

export function createMenu(data: MenuForm) {
  return post<null>('/system/menus', data)
}

export function updateMenu(id: number, data: MenuForm) {
  return put<null>(`/system/menus/${id}`, data)
}

export function deleteMenu(id: number) {
  return del<null>(`/system/menus/${id}`)
}

export function changeMenuStatus(id: number, status: number) {
  return request<null>({ url: `/system/menus/${id}/status`, method: 'put', params: { status } })
}

/** 部门 */
export interface DeptTreeNode {
  id: number
  deptName: string
  parentId: number
  sortOrder?: number
  leader?: string
  phone?: string
  email?: string
  status: number
  children?: DeptTreeNode[]
}

export interface DeptForm {
  deptName: string
  parentId: number
  sortOrder?: number
  leader?: string
  phone?: string
  email?: string
  status: number
  remark?: string
}

export function getDeptTree(params?: { deptName?: string; status?: number }) {
  return get<DeptTreeNode[]>('/system/depts/tree', params)
}

export function createDept(data: DeptForm) {
  return post<null>('/system/depts', data)
}

export function updateDept(id: number, data: DeptForm) {
  return put<null>(`/system/depts/${id}`, data)
}

export function deleteDept(id: number) {
  return del<null>(`/system/depts/${id}`)
}

export function changeDeptStatus(id: number, status: number) {
  return request<null>({ url: `/system/depts/${id}/status`, method: 'put', params: { status } })
}

/** 审计日志 */
export interface AuditLogItem {
  id: number
  userId?: number
  username?: string
  module: string
  operation: string
  method?: string
  requestUrl?: string
  requestMethod?: string
  requestParams?: string
  responseResult?: string
  ip?: string
  userAgent?: string
  status: number
  errorMessage?: string
  durationMs?: number
  createTime?: string
}

export function pageAuditLogs(params: {
  current?: number
  size?: number
  username?: string
  module?: string
  status?: number
  startTime?: string
  endTime?: string
}) {
  return get<PageResult<AuditLogItem>>('/system/audit-logs', params)
}

export function cleanAuditLogs(beforeTime: string) {
  return del<null>('/system/audit-logs/clean', { beforeTime })
}