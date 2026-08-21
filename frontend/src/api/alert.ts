import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'

// ============ 告警规则 ============

/** 告警规则 */
export interface AlertRule {
  id: number
  name: string
  hostId?: number
  hostName?: string
  metric: string
  operator: string
  threshold: number
  durationSeconds: number
  alertLevel: number
  alertLevelName?: string
  notifyChannels?: string
  notifyUsers?: string
  status: number
  statusName?: string
  createTime?: string
  remark?: string
}

/** 告警规则请求 */
export interface AlertRuleForm {
  name: string
  hostId?: number
  metric: string
  operator: string
  threshold: number
  durationSeconds?: number
  alertLevel: number
  notifyChannels?: string
  notifyUsers?: string
  status?: number
  remark?: string
}

/** 告警级别文本 */
export const ALERT_LEVEL_TEXT: Record<number, string> = {
  0: '紧急',
  1: '高',
  2: '中',
  3: '低'
}

/** 告警级别 → 标签 */
export const ALERT_LEVEL_TAG: Record<number, 'danger' | 'warning' | '' | 'info'> = {
  0: 'danger',
  1: 'warning',
  2: '',
  3: 'info'
}

/** 监控指标 */
export const METRICS = [
  { label: 'CPU 使用率', value: 'cpu_usage' },
  { label: '内存使用率', value: 'memory_usage' },
  { label: '磁盘使用率', value: 'disk_usage' }
]

/** 比较运算符 */
export const OPERATORS = ['>', '>=', '<', '<=', '==']

/** 通知渠道 */
export const CHANNELS = ['EMAIL', 'FEISHU', 'DINGTALK', 'WEBHOOK']

export function pageAlertRules(params: {
  current?: number
  size?: number
  keyword?: string
  status?: number
  hostId?: number
} = {}) {
  return get<PageResult<AlertRule>>('/alerts/rules', params)
}

export function getAlertRule(id: number) {
  return get<AlertRule>(`/alerts/rules/${id}`)
}

export function createAlertRule(data: AlertRuleForm) {
  return post<null>('/alerts/rules', data)
}

export function updateAlertRule(id: number, data: AlertRuleForm) {
  return put<null>(`/alerts/rules/${id}`, data)
}

export function deleteAlertRule(id: number) {
  return del<null>(`/alerts/rules/${id}`)
}

export function changeAlertRuleStatus(id: number, status: number) {
  return post<null>(`/alerts/rules/${id}/status`, { status })
}

// ============ 告警事件 ============

/** 告警事件 */
export interface AlertEvent {
  id: number
  ruleId: number
  ruleName?: string
  hostId?: number
  hostName?: string
  hostIp?: string
  alertLevel: number
  alertLevelName?: string
  metric: string
  currentValue: number
  threshold: number
  status: number
  statusName?: string
  confirmUserName?: string
  confirmTime?: string
  recoverTime?: string
  silenceUntil?: string
  createTime?: string
}

/** 事件状态文本 */
export const EVENT_STATUS_TEXT: Record<number, string> = {
  1: '告警中',
  2: '已确认',
  3: '已恢复',
  4: '已静默'
}

/** 事件状态 → 标签 */
export const EVENT_STATUS_TAG: Record<number, 'danger' | 'warning' | 'success' | 'info'> = {
  1: 'danger',
  2: 'warning',
  3: 'success',
  4: 'info'
}

export function pageAlertEvents(params: {
  current?: number
  size?: number
  status?: number
  alertLevel?: number
  hostId?: number
  keyword?: string
} = {}) {
  return get<PageResult<AlertEvent>>('/alerts/events', params)
}

export function getAlertEvent(id: number) {
  return get<AlertEvent>(`/alerts/events/${id}`)
}

export function confirmAlertEvent(id: number) {
  return post<null>(`/alerts/events/${id}/confirm`)
}

export function silenceAlertEvent(id: number, silenceMinutes: number) {
  return post<null>(`/alerts/events/${id}/silence`, { silenceMinutes })
}

export function recoverAlertEvent(id: number) {
  return post<null>(`/alerts/events/${id}/recover`)
}

// ============ 告警统计 ============

export interface AlertStats {
  activeAlerts: number
  todayAlerts: number
  byLevel: { urgent: number; high: number; medium: number; low: number }
  topHosts: { hostId: number; hostname?: string; alertCount: number }[]
}

export function getAlertStats() {
  return get<AlertStats>('/alerts/stats')
}

// ============ 值班排班 ============

/** 值班排班 */
export interface AlertDuty {
  id: number
  userId: number
  userName?: string
  dutyDate: string
  shiftType: number
  shiftTypeName?: string
}

/** 值班排班请求 */
export interface AlertDutyForm {
  userId: number
  dutyDate: string
  shiftType: number
}

/** 班次文本 */
export const SHIFT_TYPE_TEXT: Record<number, string> = {
  1: '全天',
  2: '白班',
  3: '夜班'
}

export function listDutyByMonth(month: string) {
  return get<AlertDuty[]>('/alerts/duty', { month })
}

export function createDuty(data: AlertDutyForm) {
  return post<null>('/alerts/duty', data)
}

export function updateDuty(id: number, data: AlertDutyForm) {
  return put<null>(`/alerts/duty/${id}`, data)
}

export function deleteDuty(id: number) {
  return del<null>(`/alerts/duty/${id}`)
}