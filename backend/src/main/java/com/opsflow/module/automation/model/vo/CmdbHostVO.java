package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 目标主机 VO
 */
@Data
public class CmdbHostVO {

    private Long id;

    private String hostname;

    private String ipAddress;

    private Integer sshPort;

    /** SSH 登录用户名 */
    private String sshUser;

    private String osType;

    private String osVersion;

    private Integer cpuCores;

    private Integer memoryGb;

    private Integer diskGb;

    /** 认证方式：1=密码 2=密钥 */
    private Integer authType;

    private String authTypeName;

    /** 状态：0=不可用 1=运行中 2=维护中 3=已退役 */
    private Integer status;

    private String statusName;

    private Long ownerId;

    private String ownerName;

    private String groupName;

    private LocalDateTime lastCheckTime;

    private LocalDateTime createTime;

    private String remark;
}