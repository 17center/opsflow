package com.opsflow.module.auth.security;

import com.opsflow.common.util.JwtUtil;
import com.opsflow.module.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer Token，校验通过后写入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthService authService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        // 存在令牌且当前无认证信息时进行校验
        if (StringUtils.hasText(token)
                && SecurityContextHolder.getContext().getAuthentication() == null
                && authService.isTokenValid(token)) {
            Claims claims = jwtUtil.parseToken(token);
            if (claims != null) {
                String username = claims.getSubject();
                Long userId = claims.get("userId") == null
                        ? null : Long.valueOf(claims.get("userId").toString());
                // 加载用户真实权限标识，支撑 @PreAuthorize("hasAuthority('...')") 方法级鉴权
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
                List<String> permissions = authService.getPermissionCodes(userId);
                if (permissions != null) {
                    permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                }
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                new LoginUser(userId, username), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头解析 Bearer Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}