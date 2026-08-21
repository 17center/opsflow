package com.opsflow.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.dto.UserCreateDTO;
import com.opsflow.module.auth.model.dto.UserUpdateDTO;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.auth.model.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户管理服务：分页查询、新增、修改、删除（逻辑删）、启停用、重置密码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** 超级管理员账号，受保护不可删除/禁用 */
    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     */
    public PageResult<UserVO> page(long current, long size, String username, String nickname,
                                   Integer status, Long deptId) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StringUtils.hasText(username), SysUser::getUsername, username)
                        .like(StringUtils.hasText(nickname), SysUser::getNickname, nickname)
                        .eq(status != null, SysUser::getStatus, status)
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .orderByDesc(SysUser::getId));
        List<UserVO> records = page.getRecords().stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 新增用户
     */
    public void create(UserCreateDTO dto, String operator) {
        // 用户名唯一校验
        Long count = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        // 默认启用
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
        user.setCreateBy(operator);
        userMapper.insert(user);
        log.info("新增用户: username={}, operator={}", dto.getUsername(), operator);
    }

    /**
     * 修改用户（用户名不可修改）
     */
    public void update(Long id, UserUpdateDTO dto, String operator) {
        SysUser user = getById(id);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus());
        user.setRemark(dto.getRemark());
        user.setUpdateBy(operator);
        userMapper.updateById(user);
    }

    /**
     * 删除用户（逻辑删除，保护超级管理员与自身）
     */
    public void delete(Long id, Long operatorId) {
        SysUser user = getById(id);
        if (ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.ADMIN_NOT_DELETABLE);
        }
        if (id.equals(operatorId)) {
            throw new BusinessException(ErrorCode.SELF_NOT_DELETABLE);
        }
        userMapper.deleteById(id);
        log.info("删除用户: id={}, username={}, operator={}", id, user.getUsername(), operatorId);
    }

    /**
     * 启停用（保护超级管理员，不允许禁用自己）
     */
    public void changeStatus(Long id, Integer status, Long operatorId) {
        SysUser user = getById(id);
        if (status != null && status == 0) {
            if (ADMIN_USERNAME.equals(user.getUsername())) {
                throw new BusinessException(ErrorCode.ADMIN_NOT_DISABLED);
            }
            if (id.equals(operatorId)) {
                throw new BusinessException(ErrorCode.SELF_NOT_DELETABLE.getCode(), "不可禁用自己");
            }
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 重置密码
     */
    public void resetPassword(Long id, String newPassword, String operator) {
        SysUser user = getById(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordErrorCount(0);
        user.setLockUntil(null);
        user.setUpdateBy(operator);
        userMapper.updateById(user);
        log.info("重置密码: id={}, username={}, operator={}", id, user.getUsername(), operator);
    }

    /**
     * 按 ID 查询用户，不存在则抛异常
     */
    private SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 实体转 VO（不暴露密码等敏感字段）
     */
    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
