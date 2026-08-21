package com.opsflow.module.automation.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 执行输出日志实体（auto_exec_log 表）
 */
@Data
@TableName("auto_exec_log")
public class AutoExecLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联执行记录 ID */
    private Long execRecordId;

    /** 输出流：1=stdout 2=stderr */
    private Integer streamType;

    /** 行号 */
    private Integer lineNumber;

    /** 输出内容（单行） */
    private String content;

    /** 输出时间戳（毫秒精度） */
    private LocalDateTime timestamp;
}