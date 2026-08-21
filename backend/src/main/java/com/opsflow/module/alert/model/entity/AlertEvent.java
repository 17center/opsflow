package com.opsflow.module.alert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警事件实体（alert_event 表）
 */
@Data
@TableName("alert_event")
public class AlertEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 触发的规则 ID */
    private Long ruleId;

    /** 触发的主机 ID */
    private Long hostId;

    /** 告警级别：0=紧急 1=高 2=中 3=低 */
    private Integer alertLevel;

    /** 指标名 */
    private String metric;

    /** 触发时的指标值 */
    private BigDecimal currentValue;

    /** 阈值 */
    private BigDecimal threshold;

    /** 状态：1=告警中 2=已确认 3=已恢复 4=已静默 */
    private Integer status;

    /** 确认人 ID */
    private Long confirmUserId;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 恢复时间 */
    private LocalDateTime recoverTime;

    /** 静默截止时间 */
    private LocalDateTime silenceUntil;

    /** 告警触发时间 */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}