package com.opsflow.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成、解析、校验令牌
 * 令牌包含 userId、username、roles 等声明，过期后由认证过滤器拦截
 */
@Component
public class JwtUtil {

    @Value("${opsflow.jwt.secret}")
    private String secret;

    @Value("${opsflow.jwt.expiration}")
    private Long expiration;

    /**
     * 生成令牌
     */
    public String generateToken(Long userId, String username, String jti) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌，返回 Claims；令牌非法/过期时返回 null
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 校验令牌是否有效（签名正确且未过期）
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 从令牌中获取用户 ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : claims.getSubject();
    }

    /**
     * 从令牌中获取 JTI（用于登出黑名单）
     */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : claims.getId();
    }

    /**
     * 获取令牌过期时间（毫秒）
     */
    public long getExpirationMs() {
        return expiration * 1000;
    }
}