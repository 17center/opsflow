import { get, post } from '@/utils/request'

/** 仪表盘数据 */
export interface ReportDashboard {
  ticketSummary: {
    total: number
    created: number
    resolved: number
    closed: number
    avgMttrHours: number
  }
  slaCompliance: {
    total: number
    breached: number
    complianceRate: number
  }
  autoExec: {
    total: number
    success: number
    failed: number
    timeout: number
    successRate: number
  }
  alertSummary: {
    total: number
    active: number
    resolved: number
    avgResolveMinutes: number
  }
  ticketTrend: { date: string; created: number; resolved: number }[]
}

/** 报表类型 */
export interface ReportTypeOption {
  value: string
  label: string
  desc: string
}

export const REPORT_TYPES: ReportTypeOption[] = [
  { value: 'ticket_stats', label: '工单统计报表', desc: '区间内工单明细（类型/优先级/状态）' },
  { value: 'sla_compliance', label: 'SLA 达标报表', desc: '已解决工单的 SLA 达标情况' },
  { value: 'auto_exec', label: '自动化执行报表', desc: '自动化脚本执行记录与结果' },
  { value: 'personal_workload', label: '个人工作量报表', desc: '各操作人的创建/解决工单统计' }
]

export function getDashboard(startTime?: string, endTime?: string) {
  return get<ReportDashboard>('/reports/dashboard', startTime || endTime ? { startTime, endTime } : {})
}

export function exportReport(data: { reportType: string; format: string; startTime?: string; endTime?: string }) {
  return post<{ downloadUrl: string }>('/reports/export', data)
}