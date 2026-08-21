package com.opsflow.module.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单权限实体（sys_menu 表）
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单 ID，0 表示顶级 */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 类型：1=目录 2=菜单 3=按钮 */
    private Integer menuType;

    /** 权限标识（如 ticket:create） */
    private String permission;

    /** 菜单图标 */
    private String icon;

    /** 是否可见：0=隐藏 1=显示 */
    private Integer visible;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}