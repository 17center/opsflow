package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户信息 VO（登录/当前用户返回）
 */
@Data
@Schema(description = "用户信息")
public class UserInfoVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "显示名称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "角色列表")
    private List<RoleVO> roles;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}