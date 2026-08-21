package com.opsflow.module.auth.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.model.dto.LoginDTO;
import com.opsflow.module.auth.model.vo.LoginVO;
import com.opsflow.module.auth.model.vo.UserInfoVO;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.service.AuthService;
import com.opsflow.module.system.annotation.AuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、刷新令牌、登出、当前用户
 */
@Tag(name = "认证授权")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @AuditLog(module = "USER", operation = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto,
                            @RequestHeader(value = "X-Forwarded-For", required = false) String xff) {
        String ip = resolveIp(xff);
        return R.ok(authService.login(dto, ip));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<Void> refresh() {
        // 刷新令牌逻辑在后续迭代完善，当前返回待实现提示
        return R.fail("刷新令牌功能开发中");
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authService.logout(authorization.substring(7));
        }
        return R.ok(null, "已登出");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public R<UserInfoVO> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return R.ok(authService.getCurrentUser(loginUser.getUserId()));
    }

    /**
     * 解析客户端 IP（优先取 X-Forwarded-For）
     */
    private String resolveIp(String xff) {
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return "unknown";
    }
}