package com.opsflow.module.automation.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 目标主机实体（cmdb_host 表）
 */
@Data
@TableName("cmdb_host")
public class CmdbHost implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主机名 */
    private String hostname;

    /** IP 地址 */
    private String ipAddress;

    /** SSH 端口 */
    private Integer sshPort;

    /** SSH 登录用户名 */
    private String sshUser;

    /** 操作系统类型 */
    private String osType;

    /** 操作系统版本 */
    private String osVersion;

    /** CPU 核数 */
    private Integer cpuCores;

    /** 内存(GB) */
    private Integer memoryGb;

    /** 磁盘(GB) */
    private Integer diskGb;

    /** 认证方式：1=密码 2=密钥 */
    private Integer authType;

    /** 凭据（AES 加密存储） */
    private String credential;

    /** 状态：0=不可用 1=运行中 2=维护中 3=已退役 */
    private Integer status;

    /** 负责人 ID */
    private Long ownerId;

    /** 主机分组 */
    private String groupName;

    /** 最后连通性检查时间 */
    private LocalDateTime lastCheckTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}