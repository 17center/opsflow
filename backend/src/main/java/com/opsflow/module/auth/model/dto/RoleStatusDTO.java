package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色启停用请求 DTO
 */
@Data
@Schema(description = "角色启停用请求")
public class RoleStatusDTO {

    @Schema(description = "状态：0=停用 1=启用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态只能为 0 或 1")
    @Max(value = 1, message = "状态只能为 0 或 1")
    private Integer status;
}