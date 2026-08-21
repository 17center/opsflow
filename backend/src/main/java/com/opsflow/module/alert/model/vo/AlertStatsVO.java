package com.opsflow.module.alert.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 告警统计 VO
 */
@Data
public class AlertStatsVO {

    /** 当前告警中数量 */
    private Long activeAlerts;

    /** 今日新增告警数量 */
    private Long todayAlerts;

    /** 按级别分布：urgent/high/medium/low */
    private Map<String, Long> byLevel;

    /** 告警最多主机 Top */
    private List<HostAlertVO> topHosts;

    /** 按级别统计条目 */
    @Data
    public static class HostAlertVO {
        private Long hostId;
        private String hostname;
        private Long alertCount;
    }
}