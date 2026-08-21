package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜单树节点 VO（供角色分配权限使用）
 */
@Data
@Schema(description = "菜单树节点")
public class MenuTreeVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "父菜单 ID")
    private Long parentId;

    @Schema(description = "类型：1=目录 2=菜单 3=按钮")
    private Integer menuType;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "子菜单")
    private List<MenuTreeVO> children;
}