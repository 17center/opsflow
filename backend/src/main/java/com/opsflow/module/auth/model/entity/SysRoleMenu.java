package com.opsflow.module.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色-菜单关联实体（sys_role_menu 表）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色 ID */
    private Long roleId;

    /** 菜单 ID */
    private Long menuId;

    /** 创建时间 */
    @JsonIgnore
    private LocalDateTime createTime;
}