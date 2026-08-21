package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表/详情 VO（不含密码等敏感字段）
 */
@Data
@Schema(description = "用户信息（管理端）")
public class UserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "显示名称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "状态：0=禁用 1=启用")
    private Integer status;

    @Schema(description = "最后登录 IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "备注")
    private String remark;
}
