package com.opsflow.module.auth.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.util.JwtUtil;
import com.opsflow.module.auth.mapper.SysMenuMapper;
import com.opsflow.module.auth.mapper.SysRoleMapper;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.mapper.SysUserRoleMapper;
import com.opsflow.module.auth.model.dto.LoginDTO;
import com.opsflow.module.auth.model.entity.SysMenu;
import com.opsflow.module.auth.model.entity.SysRole;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.auth.model.entity.SysUserRole;
import com.opsflow.module.auth.model.vo.LoginVO;
import com.opsflow.module.auth.model.vo.RoleVO;
import com.opsflow.module.auth.model.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务：登录、登出、当前用户信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 密码连续错误次数上限 */
    private static final int MAX_PASSWORD_ERROR = 5;
    /** 账号锁定时长（分钟） */
    private static final long LOCK_MINUTES = 30;
    /** JWT 黑名单 Redis key 前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:blacklist:";

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${opsflow.jwt.expiration}")
    private Long jwtExpiration;

    /**
     * 用户登录
     */
    public LoginVO login(LoginDTO dto, String ip) {
        // 1. 查询用户（不区分用户名/密码错误提示，防枚举）
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        // 2. 账号状态检查
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 3. 锁定检查
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        // 4. 密码校验
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            handlePasswordError(user);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        // 5. 登录成功，重置错误次数、更新登录信息
        resetLoginState(user);
        user.setLoginIp(ip);
        user.setLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        // 6. 生成令牌
        String jti = IdUtil.fastSimpleUUID();
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), jti);
        String refreshToken = jwtUtil.generateToken(user.getId(), user.getUsername(), IdUtil.fastSimpleUUID());

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtExpiration);
        vo.setUserInfo(buildUserInfo(user.getId()));
        return vo;
    }

    /**
     * 处理密码错误：累加错误次数，达到上限锁定账号
     */
    private void handlePasswordError(SysUser user) {
        int count = (user.getPasswordErrorCount() == null ? 0 : user.getPasswordErrorCount()) + 1;
        user.setPasswordErrorCount(count);
        if (count >= MAX_PASSWORD_ERROR) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            user.setPasswordErrorCount(0);
            log.warn("账号 {} 连续密码错误 {} 次，锁定 {} 分钟", user.getUsername(), MAX_PASSWORD_ERROR, LOCK_MINUTES);
        }
        userMapper.updateById(user);
    }

    /**
     * 登录成功重置状态
     */
    private void resetLoginState(SysUser user) {
        user.setPasswordErrorCount(0);
        user.setLockUntil(null);
    }

    /**
     * 登出：将 JTI 加入 Redis 黑名单，实现令牌即时失效
     */
    public void logout(String token) {
        String jti = jwtUtil.getJti(token);
        if (StringUtils.hasText(jti)) {
            // 黑名单有效期等于令牌剩余有效期
            long ttl = jwtUtil.getExpirationMs();
            redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + jti, "1", Duration.ofMillis(ttl));
        }
    }

    /**
     * 校验令牌是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        String jti = jwtUtil.getJti(token);
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti));
    }

    /**
     * 获取当前登录用户信息
     */
    public UserInfoVO getCurrentUser(Long userId) {
        return buildUserInfo(userId);
    }

    /**
     * 构建用户信息 VO（含角色、权限）
     */
    private UserInfoVO buildUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "用户不存在");
        }
        // 查询角色
        List<Long> roleIds = userRoleMapper.selectList(
                        Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
        List<RoleVO> roles = roleIds.isEmpty() ? List.of()
                : roleMapper.selectBatchIds(roleIds).stream()
                .filter(r -> r.getStatus() == null || r.getStatus() == 1)
                .map(r -> {
                    RoleVO vo = new RoleVO();
                    vo.setId(r.getId());
                    vo.setRoleName(r.getRoleName());
                    vo.setRoleCode(r.getRoleCode());
                    return vo;
                }).collect(Collectors.toList());
        // 查询权限标识
        List<String> permissions = menuMapper.selectPermissionsByUserId(userId);

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setDeptId(user.getDeptId());
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    /** 供登录过滤器判断令牌是否可用（未拉黑） */
    public boolean isTokenValid(String token) {
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        return !isTokenBlacklisted(token);
    }

    /**
     * 获取用户的权限标识列表（供 JWT 过滤器构建 authorities，支撑 @PreAuthorize 鉴权）
     */
    public List<String> getPermissionCodes(Long userId) {
        return menuMapper.selectPermissionsByUserId(userId);
    }
}