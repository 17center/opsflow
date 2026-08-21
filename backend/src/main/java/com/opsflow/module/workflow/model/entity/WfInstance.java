package com.opsflow.module.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 流程实例表实体（wf_instance 表）
 */
@Data
@TableName("wf_instance")
public class WfInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程定义 ID */
    private Long wfDefId;

    /** 关联工单 ID */
    private Long ticketId;

    /** Camunda 流程实例 ID */
    private String camundaInstanceId;

    /** 状态：1=运行中 2=已完成 3=已终止 */
    private Integer status;

    /** 流程发起时间 */
    private LocalDateTime startTime;

    /** 流程结束时间 */
    private LocalDateTime endTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}