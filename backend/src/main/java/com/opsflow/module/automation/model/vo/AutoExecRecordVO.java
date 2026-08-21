package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行记录 VO
 */
@Data
public class AutoExecRecordVO {

    private Long id;

    private Long scriptId;

    private String scriptName;

    private Integer scriptVersion;

    private Long hostId;

    private String hostIp;

    private String hostname;

    private Long ticketId;

    private String ticketNo;

    /** 触发方式：1=工单自动触发 2=手动触发 */
    private Integer triggerType;

    private String triggerTypeName;

    /** 状态：1=等待 2=执行中 3=成功 4=失败 5=超时 6=取消 */
    private Integer status;

    private String statusName;

    private Integer exitCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private String operatorName;

    private String errorMessage;

    private LocalDateTime createTime;
}