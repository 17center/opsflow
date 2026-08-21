package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改菜单请求 DTO
 */
@Data
@Schema(description = "菜单请求")
public class MenuDTO {

    @Schema(description = "菜单名称", example = "用户管理", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过 64")
    private String menuName;

    @Schema(description = "父菜单 ID，0 表示顶级", example = "0")
    private Long parentId;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "路由路径", example = "/system/user")
    private String path;

    @Schema(description = "前端组件路径", example = "system/user")
    private String component;

    @Schema(description = "类型：1=目录 2=菜单 3=按钮", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单类型不能为空")
    private Integer menuType;

    @Schema(description = "权限标识", example = "sys:user:manage")
    @Size(max = 128, message = "权限标识长度不能超过 128")
    private String permission;

    @Schema(description = "菜单图标", example = "User")
    @Size(max = 64, message = "图标长度不能超过 64")
    private String icon;

    @Schema(description = "是否可见：0=隐藏 1=显示", example = "1")
    private Integer visible;

    @Schema(description = "状态：0=停用 1=启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}