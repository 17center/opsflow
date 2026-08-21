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
 * 告警规则实体（alert_rule 表）
 */
@Data
@TableName("alert_rule")
public class AlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String name;

    /** 关联主机 ID（为空表示全局规则） */
    private Long hostId;

    /** 监控指标名（cpu_usage/memory_usage/disk_usage） */
    private String metric;

    /** 比较运算符：>/</>=/<=/== */
    private String operator;

    /** 阈值 */
    private BigDecimal threshold;

    /** 持续时间（秒），超过阈值持续该时间才触发 */
    private Integer durationSeconds;

    /** 告警级别：0=紧急 1=高 2=中 3=低 */
    private Integer alertLevel;

    /** 通知渠道（逗号分隔：EMAIL/FEISHU/DINGTALK/WEBHOOK） */
    private String notifyChannels;

    /** 通知人 ID 列表（逗号分隔，为空时通知值班人） */
    private String notifyUsers;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}