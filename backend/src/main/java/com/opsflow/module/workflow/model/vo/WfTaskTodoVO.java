package com.opsflow.module.workflow.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的待办任务 VO
 */
@Data
public class WfTaskTodoVO {

    private Long taskId;

    private String taskName;

    private Long wfInstanceId;

    private Long ticketId;

    private String ticketNo;

    private String ticketTitle;

    /** 签批方式：1=单人 2=会签 3=或签 */
    private Integer signType;

    private LocalDateTime dueTime;

    private LocalDateTime createTime;

    /** 审批人姓名 */
    private String assigneeName;

    /** 任务状态：1=待处理 2=已通过 3=已驳回 4=已转交 5=已超时 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 审批动作：APPROVE/REJECT/DELEGATE */
    private String action;

    /** 审批意见 */
    private String comment;

    /** 完成时间 */
    private LocalDateTime completeTime;
}