package com.opsflow.module.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.auth.mapper.SysMenuMapper;
import com.opsflow.module.auth.model.dto.MenuDTO;
import com.opsflow.module.auth.model.entity.SysMenu;
import com.opsflow.module.auth.model.vo.MenuAdminVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单管理服务：树形结构、新增、修改、删除、启停用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    /**
     * 查询菜单树（可按名称/状态过滤）
     */
    public List<MenuAdminVO> tree(String menuName, Integer status) {
        List<SysMenu> menus = menuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery()
                        .like(StringUtils.hasText(menuName), SysMenu::getMenuName, menuName)
                        .eq(status != null, SysMenu::getStatus, status)
                        .orderByAsc(SysMenu::getSortOrder)
                        .orderByAsc(SysMenu::getId));
        return buildTree(menus);
    }

    /**
     * 新增菜单
     */
    public void create(MenuDTO dto, String operator) {
        validatePermissionUnique(dto.getPermission(), null);
        validateParent(dto.getParentId(), null);
        SysMenu menu = new SysMenu();
        copyToEntity(dto, menu);
        menu.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        menu.setCreateBy(operator);
        menuMapper.insert(menu);
        log.info("新增菜单: menuName={}, operator={}", dto.getMenuName(), operator);
    }

    /**
     * 修改菜单
     */
    public void update(Long id, MenuDTO dto, String operator) {
        SysMenu menu = getById(id);
        validatePermissionUnique(dto.getPermission(), id);
        validateParent(dto.getParentId(), id);
        copyToEntity(dto, menu);
        menu.setUpdateBy(operator);
        menuMapper.updateById(menu);
    }

    /**
     * 删除菜单（存在子菜单时不可删除）
     */
    public void delete(Long id) {
        getById(id);
        Long childCount = menuMapper.selectCount(
                Wrappers.<SysMenu>lambdaQuery().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        menuMapper.deleteById(id);
        // 清理角色-菜单关联（由数据库外键 ON DELETE CASCADE 处理）
        log.info("删除菜单: id={}", id);
    }

    /**
     * 启停用菜单
     */
    public void changeStatus(Long id, Integer status) {
        SysMenu menu = getById(id);
        menu.setStatus(status);
        menuMapper.updateById(menu);
    }

    /**
     * 校验权限标识唯一（排除自身）
     */
    private void validatePermissionUnique(String permission, Long excludeId) {
        if (!StringUtils.hasText(permission)) {
            return;
        }
        Long count = menuMapper.selectCount(
                Wrappers.<SysMenu>lambdaQuery()
                        .eq(SysMenu::getPermission, permission)
                        .ne(excludeId != null, SysMenu::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.MENU_PERMISSION_EXISTS);
        }
    }

    /**
     * 校验父菜单合法性：父菜单不能是自身或其子菜单
     */
    private void validateParent(Long parentId, Long currentId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (currentId != null && parentId.equals(currentId)) {
            throw new BusinessException(ErrorCode.MENU_PARENT_INVALID);
        }
        // 校验父菜单存在
        if (menuMapper.selectById(parentId) == null) {
            throw new BusinessException(ErrorCode.MENU_PARENT_INVALID);
        }
        // 校验不构成循环（父菜单不能是当前菜单的子菜单）
        if (currentId != null) {
            List<SysMenu> all = menuMapper.selectList(null);
            Map<Long, Long> parentMap = new HashMap<>();
            for (SysMenu m : all) {
                parentMap.put(m.getId(), m.getParentId());
            }
            Long cursor = parentId;
            while (cursor != null && cursor != 0) {
                if (cursor.equals(currentId)) {
                    throw new BusinessException(ErrorCode.MENU_PARENT_INVALID);
                }
                cursor = parentMap.get(cursor);
            }
        }
    }

    /**
     * 按 ID 查询菜单，不存在则抛异常
     */
    private SysMenu getById(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    /**
     * DTO 拷贝到实体（不含审计字段）
     */
    private void copyToEntity(MenuDTO dto, SysMenu menu) {
        menu.setMenuName(dto.getMenuName());
        menu.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setMenuType(dto.getMenuType());
        menu.setPermission(dto.getPermission());
        menu.setIcon(dto.getIcon());
        menu.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        menu.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        menu.setRemark(dto.getRemark());
        if (dto.getParentId() != null) {
            menu.setParentId(dto.getParentId());
        }
    }

    /**
     * 构建树形结构
     */
    private List<MenuAdminVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuAdminVO> nodeMap = new HashMap<>();
        for (SysMenu m : menus) {
            MenuAdminVO vo = new MenuAdminVO();
            BeanUtils.copyProperties(m, vo);
            vo.setChildren(new ArrayList<>());
            nodeMap.put(m.getId(), vo);
        }
        List<MenuAdminVO> roots = new ArrayList<>();
        for (MenuAdminVO vo : nodeMap.values()) {
            MenuAdminVO parent = nodeMap.get(vo.getParentId());
            if (parent != null && !vo.getId().equals(vo.getParentId())) {
                parent.getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }
}