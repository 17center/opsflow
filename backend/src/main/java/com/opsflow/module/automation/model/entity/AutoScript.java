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
 * 自动化脚本实体（auto_script 表）
 */
@Data
@TableName("auto_script")
public class AutoScript implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 脚本名称 */
    private String name;

    /** 脚本说明 */
    private String description;

    /** 脚本类型：1=Shell 2=Python 3=Ansible Playbook */
    private Integer scriptType;

    /** 当前版本脚本内容（AES 加密存储） */
    private String content;

    /** 参数定义（JSON Schema） */
    private String paramsSchema;

    /** 默认超时时间（秒） */
    private Integer timeoutSeconds;

    /** 当前版本号 */
    private Integer currentVersion;

    /** 状态：0=停用 1=启用 */
    private Integer status;

    /** 脚本分类 */
    private String category;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}