package com.opsflow.module.alert.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 告警规则请求
 */
@Data
@Schema(description = "告警规则请求")
public class AlertRuleDTO {

    @Schema(description = "规则名称")
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过 128")
    private String name;

    @Schema(description = "关联主机 ID（为空表示全局）")
    private Long hostId;

    @Schema(description = "监控指标名")
    @NotBlank(message = "监控指标不能为空")
    @Size(max = 64, message = "指标名长度不能超过 64")
    private String metric;

    @Schema(description = "比较运算符：>/</>=/<=/==")
    @NotBlank(message = "比较运算符不能为空")
    @Pattern(regexp = ">|>=|<|<=|==", message = "比较运算符无效")
    private String operator;

    @Schema(description = "阈值")
    @NotNull(message = "阈值不能为空")
    @DecimalMin(value = "0", message = "阈值不能为负")
    private BigDecimal threshold;

    @Schema(description = "持续时间（秒）")
    @Min(value = 1, message = "持续时间至少 1 秒")
    @Max(value = 86400, message = "持续时间不能超过 86400 秒")
    private Integer durationSeconds;

    @Schema(description = "告警级别：0=紧急 1=高 2=中 3=低")
    @NotNull(message = "告警级别不能为空")
    @Min(value = 0, message = "告警级别无效")
    @Max(value = 3, message = "告警级别无效")
    private Integer alertLevel;

    @Schema(description = "通知渠道（逗号分隔：EMAIL/FEISHU/DINGTALK）")
    @Size(max = 128, message = "通知渠道长度不能超过 128")
    private String notifyChannels;

    @Schema(description = "通知人 ID 列表（逗号分隔，为空时通知值班人）")
    @Size(max = 500, message = "通知人列表长度不能超过 500")
    private String notifyUsers;

    @Schema(description = "状态：0=停用 1=启用")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}