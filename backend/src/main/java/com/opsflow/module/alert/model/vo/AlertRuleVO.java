package com.opsflow.module.alert.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则 VO
 */
@Data
public class AlertRuleVO {

    private Long id;

    private String name;

    private Long hostId;

    private String hostName;

    /** 监控指标名 */
    private String metric;

    /** 比较运算符 */
    private String operator;

    /** 阈值 */
    private BigDecimal threshold;

    /** 持续时间（秒） */
    private Integer durationSeconds;

    /** 告警级别：0=紧急 1=高 2=中 3=低 */
    private Integer alertLevel;

    private String alertLevelName;

    /** 通知渠道（逗号分隔） */
    private String notifyChannels;

    /** 通知人 ID 列表 */
    private String notifyUsers;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    private String statusName;

    private LocalDateTime createTime;

    private String remark;
}