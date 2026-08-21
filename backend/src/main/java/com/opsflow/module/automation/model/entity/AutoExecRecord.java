package com.opsflow.module.automation.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 执行记录实体（auto_exec_record 表）
 */
@Data
@TableName("auto_exec_record")
public class AutoExecRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联工单 ID */
    private Long ticketId;

    /** 脚本 ID */
    private Long scriptId;

    /** 执行时脚本版本 */
    private Integer scriptVersion;

    /** 目标主机 ID */
    private Long hostId;

    /** 触发方式：1=工单自动触发 2=手动触发 */
    private Integer triggerType;

    /** 状态：1=等待 2=执行中 3=成功 4=失败 5=超时 6=取消 */
    private Integer status;

    /** 脚本退出码 */
    private Integer exitCode;

    /** 开始执行时间 */
    private LocalDateTime startTime;

    /** 执行结束时间 */
    private LocalDateTime endTime;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 触发人 ID */
    private Long operatorId;

    /** 错误信息 */
    private String errorMessage;

    /** 已重试次数 */
    private Integer retryCount;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}