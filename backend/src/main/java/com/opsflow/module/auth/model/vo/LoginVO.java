package com.opsflow.module.auth.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "有效期（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}