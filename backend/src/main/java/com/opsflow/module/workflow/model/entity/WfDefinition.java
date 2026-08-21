package com.opsflow.module.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程定义表实体（wf_definition 表）
 */
@Data
@TableName("wf_definition")
public class WfDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程名称 */
    private String name;

    /** 流程标识（如 change_approval），key 为 MySQL 保留字需转义 */
    @TableField(value = "`key`")
    private String key;

    /** 版本号 */
    private Integer version;

    /** BPMN 2.0 XML 定义（简化节点配置 JSON） */
    private String bpmnXml;

    /** 流程描述 */
    private String description;

    /** 状态：0=草稿 1=已发布 2=已停用 */
    private Integer status;

    /** Camunda 部署 ID */
    private String camundaDeployId;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}