package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 脚本版本 VO
 */
@Data
public class AutoScriptVersionVO {

    private Long id;

    private Long scriptId;

    private Integer version;

    /** 脚本内容（已解密） */
    private String content;

    private String changeLog;

    private String createBy;

    private LocalDateTime createTime;
}