package com.opsflow.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体（sys_audit_log 表）
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID（系统自动操作为空） */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 所属模块（USER/TICKET/SCRIPT/ALERT） */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求方法（类名.方法名） */
    private String method;

    /** 请求 URL */
    private String requestUrl;

    /** HTTP 方法 */
    private String requestMethod;

    /** 请求参数（脱敏后） */
    private String requestParams;

    /** 响应结果（截断存储） */
    private String responseResult;

    /** 操作人 IP */
    private String ip;

    /** 浏览器 UA */
    private String userAgent;

    /** 操作结果：0=失败 1=成功 */
    private Integer status;

    /** 失败原因 */
    private String errorMessage;

    /** 接口耗时（毫秒） */
    private Long durationMs;

    /** 操作时间 */
    private LocalDateTime createTime;
}