package com.opsflow.common.config;

import com.opsflow.module.auth.security.JwtAuthenticationFilter;
import com.opsflow.module.auth.security.RestAccessDeniedHandler;
import com.opsflow.module.auth.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置：JWT 无状态认证 + RBAC 方法级权限
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 未认证 / 无权限统一返回 JSON
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 放行白名单：接口文档、健康检查、登录、刷新令牌、WebSocket
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/health",
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/favicon.ico",
                                "/ws/**"
                        ).permitAll()
                        // 其余请求需认证
                        .anyRequest().authenticated()
                )
                // JWT 过滤器置于用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}