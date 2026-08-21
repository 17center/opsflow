package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单树节点 VO（菜单管理）
 */
@Data
@Schema(description = "菜单树节点（管理端）")
public class MenuAdminVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "父菜单 ID")
    private Long parentId;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "类型：1=目录 2=菜单 3=按钮")
    private Integer menuType;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "是否可见：0=隐藏 1=显示")
    private Integer visible;

    @Schema(description = "状态：0=停用 1=启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子菜单")
    private List<MenuAdminVO> children;
}