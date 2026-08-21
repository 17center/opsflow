package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改角色请求 DTO（角色编码不可修改）
 */
@Data
@Schema(description = "修改角色请求")
public class RoleUpdateDTO {

    @Schema(description = "角色名称", example = "运维管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64")
    private String roleName;

    @Schema(description = "排序序号", example = "0")
    private Integer sortOrder;

    @Schema(description = "数据权限：1=全部 2=本部门 3=本部门及以下 4=仅本人", example = "1")
    private Integer dataScope;

    @Schema(description = "状态：0=停用 1=启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}