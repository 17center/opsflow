package com.opsflow.module.alert.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 告警静默请求
 */
@Data
@Schema(description = "告警静默请求")
public class AlertSilenceDTO {

    @Schema(description = "静默时长（分钟）")
    @NotNull(message = "静默时长不能为空")
    @Min(value = 1, message = "静默时长至少 1 分钟")
    @Max(value = 10080, message = "静默时长不能超过 7 天")
    private Integer silenceMinutes;
}