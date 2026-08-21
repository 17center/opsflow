import { get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'

/** 流程节点配置 */
export interface WfNode {
  nodeKey: string
  nodeName: string
  nodeType?: number
  assigneeId?: number
  candidateGroup?: string
  signType?: number
}

/** 流程定义 */
export interface WfDefinition {
  id: number
  name: string
  key: string
  version?: number
  status?: number
  statusName?: string
  description?: string
  nodes?: WfNode[]
  createTime?: string
  updateTime?: string
}

/** 创建/更新流程定义请求 */
export interface WfDefinitionForm {
  name: string
  key: string
  description?: string
  nodes: WfNode[]
}

/** 流程实例中的任务 */
export interface WfTask {
  taskId: number
  taskName: string
  nodeKey?: string
  assigneeName?: string
  status?: number
  statusName?: string
  action?: string
  comment?: string
  dueTime?: string
  completeTime?: string
}

/** 流程实例 */
export interface WfInstance {
  id: number
  wfDefId?: number
  wfDefName?: string
  wfDefVersion?: number
  ticketId?: number
  ticketNo?: string
  ticketTitle?: string
  status?: number
  statusName?: string
  startTime?: string
  endTime?: string
  tasks?: WfTask[]
}

/** 我的待办 */
export interface WfTodo {
  taskId: number
  taskName: string
  wfInstanceId: number
  ticketId: number
  ticketNo: string
  ticketTitle: string
  signType?: number
  dueTime?: string
  createTime?: string
}

/** 流程定义状态文本 */
export const DEF_STATUS_TEXT: Record<number, string> = {
  0: '草稿',
  1: '已发布',
  2: '已停用'
}

/** 流程定义状态 → 标签 */
export const DEF_STATUS_TAG: Record<number, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  0: 'info',
  1: 'success',
  2: 'danger'
}

/** 节点类型文本 */
export const NODE_TYPE_TEXT: Record<number, string> = {
  1: '人工审批',
  2: '自动执行',
  3: '通知'
}

/** 签批方式文本 */
export const SIGN_TYPE_TEXT: Record<number, string> = {
  1: '单人',
  2: '会签',
  3: '或签'
}

/** 实例状态文本 */
export const INST_STATUS_TEXT: Record<number, string> = {
  1: '运行中',
  2: '已完成',
  3: '已终止'
}

/** 实例状态 → 标签 */
export const INST_STATUS_TAG: Record<number, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  1: 'primary',
  2: 'success',
  3: 'danger'
}

/** 任务状态文本 */
export const TASK_STATUS_TEXT: Record<number, string> = {
  1: '待处理',
  2: '已通过',
  3: '已驳回',
  4: '已转交',
  5: '已超时'
}

/** 任务状态 → 标签 */
export const TASK_STATUS_TAG: Record<number, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: 'info',
  5: 'danger'
}

// ============ 流程定义 ============

/** 分页查询流程定义 */
export function pageDefinitions(params: {
  current?: number
  size?: number
  key?: string
  name?: string
  status?: number
} = {}) {
  return get<PageResult<WfDefinition>>('/workflow/definitions', params)
}

/** 流程定义详情 */
export function getDefinition(id: number) {
  return get<WfDefinition>(`/workflow/definitions/${id}`)
}

/** 创建流程定义 */
export function createDefinition(data: WfDefinitionForm) {
  return post<null>('/workflow/definitions', data)
}

/** 修改流程定义 */
export function updateDefinition(id: number, data: WfDefinitionForm) {
  return put<null>(`/workflow/definitions/${id}`, data)
}

/** 发布流程定义 */
export function publishDefinition(id: number) {
  return post<null>(`/workflow/definitions/${id}/publish`)
}

/** 停用流程定义 */
export function disableDefinition(id: number) {
  return post<null>(`/workflow/definitions/${id}/disable`)
}

// ============ 流程实例 ============

/** 启动流程实例 */
export function startInstance(data: { ticketId: number; definitionId: number }) {
  return post<{ id: number }>('/workflow/instances', data)
}

/** 分页查询流程实例 */
export function pageInstances(params: {
  current?: number
  size?: number
  ticketId?: number
  status?: number
} = {}) {
  return get<PageResult<WfInstance>>('/workflow/instances', params)
}

/** 流程实例详情 */
export function getInstance(id: number) {
  return get<WfInstance>(`/workflow/instances/${id}`)
}

// ============ 审批任务 ============

/** 我的待办 */
export function pageTodo(params: { current?: number; size?: number } = {}) {
  return get<PageResult<WfTodo>>('/workflow/tasks/todo', params)
}

/** 审批通过 */
export function approveTask(taskId: number, comment: string) {
  return post<null>(`/workflow/tasks/${taskId}/approve`, { comment })
}

/** 审批驳回 */
export function rejectTask(taskId: number, comment: string) {
  return post<null>(`/workflow/tasks/${taskId}/reject`, { comment })
}

/** 转交审批 */
export function delegateTask(taskId: number, targetUserId: number, comment: string) {
  return post<null>(`/workflow/tasks/${taskId}/delegate`, { targetUserId, comment })
}