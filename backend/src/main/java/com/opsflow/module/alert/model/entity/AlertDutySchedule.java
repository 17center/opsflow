package com.opsflow.module.alert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 值班排班实体（alert_duty_schedule 表）
 */
@Data
@TableName("alert_duty_schedule")
public class AlertDutySchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 值班人 ID */
    private Long userId;

    /** 值班日期 */
    private LocalDate dutyDate;

    /** 班次：1=全天 2=白班 3=夜班 */
    private Integer shiftType;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}