package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息 VO（管理端）
 */
@Data
@Schema(description = "角色信息（管理端）")
public class RoleAdminVO {

    @Schema(description = "角色 ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "状态：0=停用 1=启用")
    private Integer status;

    @Schema(description = "数据权限：1=全部 2=本部门 3=本部门及以下 4=仅本人")
    private Integer dataScope;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "已分配的菜单 ID 列表（详情接口返回）")
    private List<Long> menuIds;
}