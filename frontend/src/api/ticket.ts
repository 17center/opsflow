import { get, post, put } from '@/utils/request'
import type { PageResult } from '@/types/api'

/** 工单列表项 */
export interface TicketItem {
  id: number
  ticketNo: string
  title: string
  ticketType: number
  ticketTypeName?: string
  priority: number
  priorityName?: string
  status: string
  statusName?: string
  creatorName?: string
  assigneeName?: string
  slaBreached?: boolean
  createTime?: string
}

/** 用户简要信息 */
export interface UserRef {
  id: number
  nickname: string
}

/** 工单评论 */
export interface CommentItem {
  id: number
  user: UserRef
  content: string
  createTime: string
}

/** 工单附件 */
export interface AttachmentItem {
  id: number
  fileName: string
  fileSize: number
  filePath?: string
  uploadTime: string
}

/** 工单操作日志 */
export interface LogItem {
  action: string
  operatorName: string
  content?: string
  createTime: string
}

/** 工单详情 */
export interface TicketDetail {
  id: number
  ticketNo: string
  title: string
  description?: string
  ticketType: number
  ticketTypeName?: string
  priority: number
  priorityName?: string
  status: string
  statusName?: string
  creator?: UserRef
  assignee?: UserRef
  hostId?: number
  scriptId?: number
  scriptParams?: Record<string, unknown>
  wfInstanceId?: number
  slaDeadline?: string
  slaResponseDeadline?: string
  slaBreached?: boolean
  comments: CommentItem[]
  attachments: AttachmentItem[]
  logs: LogItem[]
  createTime: string
}

/** 创建工单请求 */
export interface TicketCreateForm {
  title: string
  description?: string
  ticketType: number
  priority: number
  templateId?: number
  hostId?: number
  scriptId?: number
  scriptParams?: Record<string, unknown>
}

/** 看板统计 */
export interface DashboardData {
  total: number
  pendingApproval: number
  inProgress: number
  resolved: number
  byType: Record<string, number>
  byPriority: Record<string, number>
  avgMttrHours: number
  slaComplianceRate: number
}

/** 站内通知 */
export interface NotificationItem {
  id: number
  title: string
  content?: string
  notifyType: number
  relatedId?: number
  relatedType?: string
  isRead: number
  createTime: string
}

/** 状态文本映射 */
export const STATUS_TEXT: Record<string, string> = {
  DRAFT: '草稿',
  PENDING_APPROVAL: '待审批',
  APPROVED: '审批通过',
  REJECTED: '已驳回',
  PENDING_ASSIGN: '待指派',
  IN_PROGRESS: '处理中',
  EXECUTING: '执行中',
  EXEC_SUCCESS: '执行成功',
  EXEC_FAILED: '执行失败',
  RESOLVED: '已解决',
  REOPENED: '重新打开',
  CLOSED: '已关闭'
}

/** 状态 → 标签类型 */
export const STATUS_TAG: Record<string, 'info' | 'primary' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'info',
  PENDING_APPROVAL: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  PENDING_ASSIGN: 'warning',
  IN_PROGRESS: 'primary',
  EXECUTING: 'primary',
  EXEC_SUCCESS: 'success',
  EXEC_FAILED: 'danger',
  RESOLVED: 'success',
  REOPENED: 'warning',
  CLOSED: 'info'
}

/** 工单简要信息（用于工单转知识选择） */
export interface TicketBasic {
  id: number
  ticketNo: string
  title: string
  status: string
}

/** 分页查询已关闭工单（工单转知识用） */
export function pageClosedTickets(params: { current?: number; size?: number; keyword?: string } = {}) {
  return get<PageResult<TicketBasic>>('/tickets', { ...params, status: 'CLOSED' } as object)
}

/** 分页查询工单 */
export function pageTickets(params: {
  current?: number
  size?: number
  keyword?: string
  status?: string
  ticketType?: number
  priority?: number
  creatorId?: number
  assigneeId?: number
  startTime?: string
  endTime?: string
} = {}) {
  return get<PageResult<TicketItem>>('/tickets', params)
}

/** 工单详情 */
export function getTicket(id: number) {
  return get<TicketDetail>(`/tickets/${id}`)
}

/** 创建工单 */
export function createTicket(data: TicketCreateForm) {
  return post<{ id: number; ticketNo: string }>('/tickets', data)
}

/** 提交工单 */
export function submitTicket(id: number) {
  return post<null>(`/tickets/${id}/submit`)
}

/** 指派工单 */
export function assignTicket(id: number, assigneeId: number) {
  return post<null>(`/tickets/${id}/assign`, { assigneeId })
}

/** 解决工单 */
export function resolveTicket(id: number, resolution: string) {
  return post<null>(`/tickets/${id}/resolve`, { resolution })
}

/** 关闭工单 */
export function closeTicket(id: number) {
  return post<null>(`/tickets/${id}/close`)
}

/** 重新打开工单 */
export function reopenTicket(id: number, reason: string) {
  return post<null>(`/tickets/${id}/reopen`, { reason })
}

/** 看板统计 */
export function getDashboard() {
  return get<DashboardData>('/tickets/dashboard')
}

/** 工单评论列表 */
export function pageComments(ticketId: number) {
  return get<CommentItem[]>(`/tickets/${ticketId}/comments`)
}

/** 发表评论 */
export function addComment(ticketId: number, content: string, mentionedUserIds?: number[]) {
  return post<null>(`/tickets/${ticketId}/comments`, { content, mentionedUserIds })
}

/** 上传附件 */
export function uploadAttachment(ticketId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return post<AttachmentItem>(`/tickets/${ticketId}/attachments`, form as unknown as Record<string, unknown>)
}

// ============ 站内通知 ============

/** 分页查询通知 */
export function pageNotifications(params: { current?: number; size?: number; isRead?: number } = {}) {
  return get<PageResult<NotificationItem>>('/system/notifications', params)
}

/** 未读通知数 */
export function getUnreadCount() {
  return get<{ count: number }>('/system/notifications/unread-count')
}

/** 标记已读 */
export function markRead(id: number) {
  return put<null>(`/system/notifications/${id}/read`)
}

/** 全部已读 */
export function markAllRead() {
  return put<null>('/system/notifications/read-all')
}

/** 获取用户列表（用于指派） */
export function pageUsers(params: { current?: number; size?: number; status?: number } = {}) {
  return get<PageResult<{ id: number; nickname: string; username: string }>>('/system/users', params)
}