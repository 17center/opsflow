package com.opsflow.module.alert.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警事件 VO
 */
@Data
public class AlertEventVO {

    private Long id;

    private Long ruleId;

    private String ruleName;

    private Long hostId;

    private String hostName;

    private String hostIp;

    /** 告警级别：0=紧急 1=高 2=中 3=低 */
    private Integer alertLevel;

    private String alertLevelName;

    /** 指标名 */
    private String metric;

    /** 触发时指标值 */
    private BigDecimal currentValue;

    /** 阈值 */
    private BigDecimal threshold;

    /** 状态：1=告警中 2=已确认 3=已恢复 4=已静默 */
    private Integer status;

    private String statusName;

    private Long confirmUserId;

    private String confirmUserName;

    private LocalDateTime confirmTime;

    private LocalDateTime recoverTime;

    private LocalDateTime silenceUntil;

    /** 告警触发时间 */
    private LocalDateTime createTime;
}