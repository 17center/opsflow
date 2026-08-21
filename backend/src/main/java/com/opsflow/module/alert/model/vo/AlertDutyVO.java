package com.opsflow.module.alert.model.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 值班排班 VO
 */
@Data
public class AlertDutyVO {

    private Long id;

    private Long userId;

    private String userName;

    private LocalDate dutyDate;

    /** 班次：1=全天 2=白班 3=夜班 */
    private Integer shiftType;

    private String shiftTypeName;
}