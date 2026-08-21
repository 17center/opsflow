package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部门新增/修改请求 DTO
 */
@Data
@Schema(description = "部门新增/修改请求")
public class DeptDTO {

    @Schema(description = "部门名称", example = "运维组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 64, message = "部门名称长度不能超过 64")
    private String deptName;

    @Schema(description = "父部门ID，0表示顶级", example = "1")
    private Long parentId;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "负责人", example = "刘俊")
    @Size(max = 64, message = "负责人长度不能超过 64")
    private String leader;

    @Schema(description = "联系电话", example = "13800000000")
    @Size(max = 20, message = "联系电话长度不能超过 20")
    private String phone;

    @Schema(description = "联系邮箱", example = "ops@xxx.com")
    @Size(max = 128, message = "联系邮箱长度不能超过 128")
    private String email;

    @Schema(description = "状态：0=停用 1=启用", example = "1")
    private Integer status;

    @Schema(description = "备注", example = "")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}