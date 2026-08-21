package com.opsflow.module.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增用户请求 DTO
 */
@Data
@Schema(description = "新增用户请求")
public class UserCreateDTO {

    @Schema(description = "登录账号", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @Schema(description = "初始密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间")
    private String password;

    @Schema(description = "显示名称", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "显示名称不能为空")
    @Size(max = 64, message = "显示名称长度不能超过 64")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@opsflow.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "状态：0=禁用 1=启用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
