package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 执行输出日志 VO
 */
@Data
public class AutoExecLogVO {

    private Long id;

    /** 输出流：1=stdout 2=stderr */
    private Integer streamType;

    private String streamTypeName;

    private Integer lineNumber;

    private String content;

    private LocalDateTime timestamp;
}