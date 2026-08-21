package com.opsflow.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.auth.mapper.SysMenuMapper;
import com.opsflow.module.auth.mapper.SysRoleMapper;
import com.opsflow.module.auth.mapper.SysRoleMenuMapper;
import com.opsflow.module.auth.mapper.SysUserRoleMapper;
import com.opsflow.module.auth.model.dto.RoleCreateDTO;
import com.opsflow.module.auth.model.dto.RoleUpdateDTO;
import com.opsflow.module.auth.model.entity.SysMenu;
import com.opsflow.module.auth.model.entity.SysRole;
import com.opsflow.module.auth.model.entity.SysRoleMenu;
import com.opsflow.module.auth.model.entity.SysUserRole;
import com.opsflow.module.auth.model.vo.MenuTreeVO;
import com.opsflow.module.auth.model.vo.RoleAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理服务：分页查询、详情、新增、修改、删除、启停用、分配权限、菜单树
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    /** 内置管理员角色编码，受保护不可删除/停用 */
    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 分页查询角色列表
     */
    public PageResult<RoleAdminVO> page(long current, long size, String roleName, String roleCode, Integer status) {
        Page<SysRole> page = roleMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysRole>lambdaQuery()
                        .like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
                        .like(StringUtils.hasText(roleCode), SysRole::getRoleCode, roleCode)
                        .eq(status != null, SysRole::getStatus, status)
                        .orderByAsc(SysRole::getSortOrder)
                        .orderByAsc(SysRole::getId));
        List<RoleAdminVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 角色详情（含已分配菜单 ID）
     */
    public RoleAdminVO detail(Long id) {
        SysRole role = getById(id);
        RoleAdminVO vo = toVO(role);
        List<Long> menuIds = roleMenuMapper.selectList(
                        Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id))
                .stream().map(SysRoleMenu::getMenuId).toList();
        vo.setMenuIds(menuIds);
        return vo;
    }

    /**
     * 新增角色
     */
    public void create(RoleCreateDTO dto, String operator) {
        if (roleCodeExists(dto.getRoleCode(), null)) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleCode(dto.getRoleCode());
        role.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        role.setDataScope(dto.getDataScope() == null ? 1 : dto.getDataScope());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setRemark(dto.getRemark());
        role.setCreateBy(operator);
        roleMapper.insert(role);
        log.info("新增角色: roleName={}, roleCode={}, operator={}", dto.getRoleName(), dto.getRoleCode(), operator);
    }

    /**
     * 修改角色（角色编码不可修改）
     */
    public void update(Long id, RoleUpdateDTO dto, String operator) {
        SysRole role = getById(id);
        role.setRoleName(dto.getRoleName());
        role.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        role.setDataScope(dto.getDataScope() == null ? 1 : dto.getDataScope());
        role.setStatus(dto.getStatus());
        role.setRemark(dto.getRemark());
        role.setUpdateBy(operator);
        roleMapper.updateById(role);
    }

    /**
     * 删除角色（保护内置管理员角色；存在关联用户时不可删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);
        if (ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_PROTECTED);
        }
        Long userCount = userRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS);
        }
        roleMapper.deleteById(id);
        // 清理角色-菜单关联
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        log.info("删除角色: id={}, roleCode={}", id, role.getRoleCode());
    }

    /**
     * 启停用（保护内置管理员角色）
     */
    public void changeStatus(Long id, Integer status) {
        SysRole role = getById(id);
        if (status != null && status == 0 && ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.ROLE_ADMIN_PROTECTED);
        }
        role.setStatus(status);
        roleMapper.updateById(role);
    }

    /**
     * 分配权限（先清空后重建角色-菜单关联）
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long id, List<Long> menuIds) {
        getById(id);
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, id));
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(id);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        log.info("分配权限: roleId={}, menuCount={}", id, menuIds == null ? 0 : menuIds.size());
    }

    /**
     * 查询全部启用菜单树（供分配权限使用）
     */
    public List<MenuTreeVO> menuTree() {
        List<SysMenu> menus = menuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSortOrder));
        Map<Long, MenuTreeVO> nodeMap = new HashMap<>();
        for (SysMenu m : menus) {
            MenuTreeVO vo = new MenuTreeVO();
            vo.setId(m.getId());
            vo.setMenuName(m.getMenuName());
            vo.setParentId(m.getParentId());
            vo.setMenuType(m.getMenuType());
            vo.setPermission(m.getPermission());
            vo.setSortOrder(m.getSortOrder());
            vo.setChildren(new ArrayList<>());
            nodeMap.put(m.getId(), vo);
        }
        List<MenuTreeVO> roots = new ArrayList<>();
        for (MenuTreeVO vo : nodeMap.values()) {
            MenuTreeVO parent = nodeMap.get(vo.getParentId());
            if (parent != null && !vo.getId().equals(vo.getParentId())) {
                parent.getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    /**
     * 角色编码是否已存在（排除自身）
     */
    private boolean roleCodeExists(String roleCode, Long excludeId) {
        Long count = roleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery()
                        .eq(SysRole::getRoleCode, roleCode)
                        .ne(excludeId != null, SysRole::getId, excludeId));
        return count != null && count > 0;
    }

    /**
     * 按 ID 查询角色，不存在则抛异常
     */
    private SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    /**
     * 实体转 VO
     */
    private RoleAdminVO toVO(SysRole role) {
        RoleAdminVO vo = new RoleAdminVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }
}