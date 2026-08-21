package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色信息 VO
 */
@Data
@Schema(description = "角色信息")
public class RoleVO {

    @Schema(description = "角色 ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;
}