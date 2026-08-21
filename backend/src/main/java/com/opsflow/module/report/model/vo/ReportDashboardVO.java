package com.opsflow.module.report.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 运维仪表盘数据 VO
 */
@Data
public class ReportDashboardVO {

    /** 工单统计 */
    private TicketSummary ticketSummary;

    /** SLA 达标统计 */
    private SlaCompliance slaCompliance;

    /** 自动化执行统计 */
    private AutoExec autoExec;

    /** 告警统计 */
    private AlertSummary alertSummary;

    /** 工单每日趋势 */
    private List<TrendItem> ticketTrend;

    @Data
    public static class TicketSummary {
        private Long total;
        private Long created;
        private Long resolved;
        private Long closed;
        private Double avgMttrHours;
    }

    @Data
    public static class SlaCompliance {
        private Long total;
        private Long breached;
        private Double complianceRate;
    }

    @Data
    public static class AutoExec {
        private Long total;
        private Long success;
        private Long failed;
        private Long timeout;
        private Double successRate;
    }

    @Data
    public static class AlertSummary {
        private Long total;
        private Long active;
        private Long resolved;
        private Double avgResolveMinutes;
    }

    @Data
    public static class TrendItem {
        private String date;
        private Long created;
        private Long resolved;
    }
}