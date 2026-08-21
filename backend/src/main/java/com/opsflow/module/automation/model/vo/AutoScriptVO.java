package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脚本 VO
 */
@Data
public class AutoScriptVO {

    private Long id;

    private String name;

    private String description;

    private Integer scriptType;

    private String scriptTypeName;

    /** 脚本内容（已解密） */
    private String content;

    private String paramsSchema;

    private Integer timeoutSeconds;

    private Integer currentVersion;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    private String statusName;

    private String category;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}