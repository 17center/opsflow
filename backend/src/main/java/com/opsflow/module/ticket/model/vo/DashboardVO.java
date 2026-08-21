package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.util.Map;

/**
 * 工单看板统计 VO
 */
@Data
public class DashboardVO {

    /** 工单总数 */
    private Long total;

    /** 待审批数 */
    private Long pendingApproval;

    /** 处理中数 */
    private Long inProgress;

    /** 已解决数 */
    private Long resolved;

    /** 按类型分布 */
    private Map<String, Long> byType;

    /** 按优先级分布 */
    private Map<String, Long> byPriority;

    /** 平均解决时长（小时） */
    private Double avgMttrHours;

    /** SLA 达标率 */
    private Double slaComplianceRate;
}