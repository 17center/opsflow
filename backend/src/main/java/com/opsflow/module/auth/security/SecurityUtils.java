package com.opsflow.module.auth.security;

import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未认证时抛出业务异常
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }
}
