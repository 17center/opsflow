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
 * 脚本版本实体（auto_script_version 表）
 */
@Data
@TableName("auto_script_version")
public class AutoScriptVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属脚本 ID */
    private Long scriptId;

    /** 版本号 */
    private Integer version;

    /** 该版本脚本内容（AES 加密存储） */
    private String content;

    /** 变更说明 */
    private String changeLog;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}