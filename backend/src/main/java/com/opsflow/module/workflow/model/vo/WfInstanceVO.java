package com.opsflow.module.workflow.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程实例详情 VO
 */
@Data
public class WfInstanceVO {

    private Long id;

    private Long wfDefId;

    private String wfDefName;

    private Integer wfDefVersion;

    private Long ticketId;

    private String ticketNo;

    private String ticketTitle;

    /** 状态：1=运行中 2=已完成 3=已终止 */
    private Integer status;

    private String statusName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<WfTaskVO> tasks;
}