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
 * 部门实体（sys_dept 表）
 */
@Data
@TableName("sys_dept")
public class SysDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 部门名称 */
    private String deptName;

    /** 父部门 ID，0 表示顶级 */
    private Long parentId;

    /** 排序序号 */
    private Integer sortOrder;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 联系邮箱 */
    private String email;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}