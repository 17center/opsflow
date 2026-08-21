import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'

// ============ 脚本仓库 ============

/** 脚本 */
export interface AutoScript {
  id: number
  name: string
  description?: string
  scriptType: number
  scriptTypeName?: string
  content: string
  paramsSchema?: string
  timeoutSeconds: number
  currentVersion?: number
  status: number
  statusName?: string
  category?: string
  createTime?: string
  updateTime?: string
}

/** 创建/更新脚本请求 */
export interface AutoScriptForm {
  name: string
  description?: string
  scriptType: number
  content: string
  paramsSchema?: string
  timeoutSeconds: number
  category?: string
  changeLog?: string
}

/** 脚本版本 */
export interface AutoScriptVersion {
  id: number
  scriptId: number
  version: number
  content: string
  changeLog?: string
  createBy?: string
  createTime?: string
}

/** 脚本类型文本 */
export const SCRIPT_TYPE_TEXT: Record<number, string> = {
  1: 'Shell',
  2: 'Python',
  3: 'Ansible'
}

/** 脚本状态文本 */
export const SCRIPT_STATUS_TEXT: Record<number, string> = {
  0: '停用',
  1: '启用'
}

export function pageScripts(params: {
  current?: number
  size?: number
  keyword?: string
  scriptType?: number
  status?: number
} = {}) {
  return get<PageResult<AutoScript>>('/automation/scripts', params)
}

export function getScript(id: number) {
  return get<AutoScript>(`/automation/scripts/${id}`)
}

export function createScript(data: AutoScriptForm) {
  return post<null>('/automation/scripts', data)
}

export function updateScript(id: number, data: AutoScriptForm) {
  return put<null>(`/automation/scripts/${id}`, data)
}

export function deleteScript(id: number) {
  return del<null>(`/automation/scripts/${id}`)
}

export function enableScript(id: number) {
  return post<null>(`/automation/scripts/${id}/enable`)
}

export function disableScript(id: number) {
  return post<null>(`/automation/scripts/${id}/disable`)
}

export function scriptVersions(id: number) {
  return get<AutoScriptVersion[]>(`/automation/scripts/${id}/versions`)
}

export function rollbackScript(id: number, version: number) {
  return post<null>(`/automation/scripts/${id}/rollback/${version}`)
}

// ============ 目标主机 ============

/** 主机 */
export interface CmdbHost {
  id: number
  hostname: string
  ipAddress: string
  sshPort: number
  sshUser?: string
  osType?: string
  osVersion?: string
  cpuCores?: number
  memoryGb?: number
  diskGb?: number
  authType: number
  authTypeName?: string
  status: number
  statusName?: string
  ownerId?: number
  ownerName?: string
  groupName?: string
  lastCheckTime?: string
  remark?: string
  createTime?: string
}

/** 创建/更新主机请求 */
export interface CmdbHostForm {
  hostname: string
  ipAddress: string
  sshPort: number
  sshUser?: string
  osType?: string
  authType: number
  credential: string
  ownerId?: number
  groupName?: string
  remark?: string
}

/** 认证方式文本 */
export const AUTH_TYPE_TEXT: Record<number, string> = {
  1: '密码',
  2: '密钥'
}

/** 主机状态文本 */
export const HOST_STATUS_TEXT: Record<number, string> = {
  0: '不可用',
  1: '运行中',
  2: '维护中',
  3: '已退役'
}

export function pageHosts(params: {
  current?: number
  size?: number
  keyword?: string
  status?: number
  groupName?: string
} = {}) {
  return get<PageResult<CmdbHost>>('/automation/hosts', params)
}

export function getHost(id: number) {
  return get<CmdbHost>(`/automation/hosts/${id}`)
}

export function createHost(data: CmdbHostForm) {
  return post<null>('/automation/hosts', data)
}

export function updateHost(id: number, data: CmdbHostForm) {
  return put<null>(`/automation/hosts/${id}`, data)
}

export function deleteHost(id: number) {
  return del<null>(`/automation/hosts/${id}`)
}

export function testHost(id: number) {
  return post<{ result: string }>(`/automation/hosts/${id}/test`)
}

// ============ 执行引擎 ============

/** 执行记录 */
export interface AutoExecRecord {
  id: number
  scriptId: number
  scriptName?: string
  scriptVersion?: number
  hostId: number
  hostIp?: string
  hostname?: string
  ticketId?: number
  ticketNo?: string
  triggerType: number
  triggerTypeName?: string
  status: number
  statusName?: string
  exitCode?: number
  startTime?: string
  endTime?: string
  durationMs?: number
  operatorName?: string
  errorMessage?: string
  createTime?: string
}

/** 执行输出日志 */
export interface AutoExecLog {
  id: number
  streamType: number
  streamTypeName?: string
  lineNumber: number
  content: string
  timestamp?: string
}

/** 执行详情 */
export interface AutoExecDetail {
  record: AutoExecRecord
  logs: AutoExecLog[]
}

/** 触发执行请求 */
export interface AutoExecStartForm {
  scriptId: number
  hostId: number
  triggerType?: number
  ticketId?: number
}

/** 执行状态文本 */
export const EXEC_STATUS_TEXT: Record<number, string> = {
  1: '等待',
  2: '执行中',
  3: '成功',
  4: '失败',
  5: '超时',
  6: '取消'
}

/** 执行状态 → 标签 */
export const EXEC_STATUS_TAG: Record<number, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  1: 'info',
  2: 'primary',
  3: 'success',
  4: 'danger',
  5: 'warning',
  6: 'info'
}

/** 触发方式文本 */
export const TRIGGER_TYPE_TEXT: Record<number, string> = {
  1: '工单自动触发',
  2: '手动触发'
}

export function pageExecs(params: {
  current?: number
  size?: number
  scriptId?: number
  hostId?: number
  status?: number
} = {}) {
  return get<PageResult<AutoExecRecord>>('/automation/exec', params)
}

export function getExecDetail(id: number) {
  return get<AutoExecDetail>(`/automation/exec/${id}`)
}

export function startExec(data: AutoExecStartForm) {
  return post<{ id: number }>('/automation/exec/start', data)
}

export function cancelExec(id: number) {
  return post<null>(`/automation/exec/${id}/cancel`)
}