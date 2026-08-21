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
 * 角色实体（sys_role 表）
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码（如 ROLE_ADMIN） */
    private String roleCode;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    /** 数据权限范围：1=全部 2=本部门 3=本部门及以下 4=仅本人 */
    private Integer dataScope;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}