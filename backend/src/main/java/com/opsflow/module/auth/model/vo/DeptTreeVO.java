package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树形 VO
 */
@Data
@Schema(description = "部门树节点")
public class DeptTreeVO {

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "状态：0=停用 1=启用")
    private Integer status;

    @Schema(description = "子部门")
    private List<DeptTreeVO> children = new ArrayList<>();
}