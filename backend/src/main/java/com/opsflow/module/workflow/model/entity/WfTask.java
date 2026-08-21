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
 * 审批任务表实体（wf_task 表）
 */
@Data
@TableName("wf_task")
public class WfTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流程实例 ID */
    private Long wfInstanceId;

    /** BPMN 节点标识 */
    private String nodeKey;

    /** 节点名称 */
    private String nodeName;

    /** 节点类型：1=人工审批 2=自动执行 3=通知 */
    private Integer nodeType;

    /** 指定审批人 ID */
    private Long assigneeId;

    /** 候选审批角色 */
    private String candidateGroup;

    /** 签批方式：1=单人 2=会签 3=或签 */
    private Integer signType;

    /** 状态：1=待处理 2=已通过 3=已驳回 4=已转交 5=已超时 */
    private Integer status;

    /** 审批动作：APPROVE/REJECT/DELEGATE/ADD_SIGN */
    private String action;

    /** 审批意见 */
    private String comment;

    /** 超时时间（小时） */
    private Integer timeoutHours;

    /** 截止时间 */
    private LocalDateTime dueTime;

    /** 实际完成时间 */
    private LocalDateTime completeTime;

    /** 会签序号 */
    private Integer signOrder;

    /** 已签批人数 */
    private Integer signedCount;

    /** 需签批总人数 */
    private Integer requiredCount;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}