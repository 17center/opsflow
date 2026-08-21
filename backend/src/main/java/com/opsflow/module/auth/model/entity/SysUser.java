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
 * 用户实体（sys_user 表）
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 加密后的密码（序列化时忽略，防止泄露） */
    @JsonIgnore
    private String password;

    /** 显示名称 */
    private String nickname;

    /** 邮箱地址 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatar;

    /** 所属部门 ID */
    private Long deptId;

    /** 状态：0=禁用 1=启用 */
    private Integer status;

    /** 最后登录 IP */
    private String loginIp;

    /** 最后登录时间 */
    private LocalDateTime loginTime;

    /** 连续密码错误次数 */
    private Integer passwordErrorCount;

    /** 锁定截止时间 */
    private LocalDateTime lockUntil;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 备注 */
    private String remark;
}