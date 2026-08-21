package com.opsflow.module.auth.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户信息（存入 SecurityContext 的 principal）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;
}