package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限请求 DTO
 */
@Data
@Schema(description = "角色分配权限请求")
public class RoleAssignDTO {

    @Schema(description = "分配的菜单 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单 ID 列表不能为空")
    private List<Long> menuIds;
}