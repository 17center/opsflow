package com.opsflow.module.system.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志查询 VO
 */
@Data
@Schema(description = "操作审计日志")
public class AuditLogVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "所属模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "请求方法(类名.方法名)")
    private String method;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "HTTP方法")
    private String requestMethod;

    @Schema(description = "请求参数(脱敏后)")
    private String requestParams;

    @Schema(description = "响应结果")
    private String responseResult;

    @Schema(description = "操作人IP")
    private String ip;

    @Schema(description = "操作结果: 0=失败 1=成功")
    private Integer status;

    @Schema(description = "失败原因")
    private String errorMessage;

    @Schema(description = "接口耗时(毫秒)")
    private Long durationMs;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}