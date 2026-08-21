package com.opsflow.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.auth.model.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单权限 Mapper
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户 ID 查询其全部权限标识
     */
    @Select("""
            SELECT DISTINCT m.permission
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId}
              AND m.status = 1 AND m.deleted = 0
              AND m.permission IS NOT NULL AND m.permission != ''
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询其菜单（供前端动态路由使用）
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId}
              AND m.status = 1 AND m.deleted = 0
            ORDER BY m.sort_order
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}