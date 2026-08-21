package com.opsflow.module.workflow.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例中的任务 VO
 */
@Data
public class WfTaskVO {

    private Long taskId;

    private String taskName;

    private String nodeKey;

    private String assigneeName;

    /** 状态：1=待处理 2=已通过 3=已驳回 4=已转交 5=已超时 */
    private Integer status;

    private String statusName;

    private String action;

    private String comment;

    private LocalDateTime dueTime;

    private LocalDateTime completeTime;
}