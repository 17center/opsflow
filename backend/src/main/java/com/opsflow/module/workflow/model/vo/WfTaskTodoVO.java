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
}