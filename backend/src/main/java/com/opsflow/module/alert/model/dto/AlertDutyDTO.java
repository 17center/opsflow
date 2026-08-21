package com.opsflow.module.alert.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 值班排班请求
 */
@Data
@Schema(description = "值班排班请求")
public class AlertDutyDTO {

    @Schema(description = "值班人 ID")
    @NotNull(message = "值班人不能为空")
    private Long userId;

    @Schema(description = "值班日期")
    @NotNull(message = "值班日期不能为空")
    private LocalDate dutyDate;

    @Schema(description = "班次：1=全天 2=白班 3=夜班")
    @NotNull(message = "班次不能为空")
    @Min(value = 1, message = "班次无效")
    @Max(value = 3, message = "班次无效")
    private Integer shiftType;
}