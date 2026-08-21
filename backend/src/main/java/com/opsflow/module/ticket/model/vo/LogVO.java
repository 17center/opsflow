package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单操作日志 VO
 */
@Data
public class LogVO {

    private String action;

    private String operatorName;

    private String content;

    private LocalDateTime createTime;
}