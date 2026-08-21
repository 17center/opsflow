package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单列表项 VO
 */
@Data
public class TicketVO {

    private Long id;

    private String ticketNo;

    private String title;

    private Integer ticketType;

    private String ticketTypeName;

    private Integer priority;

    private String priorityName;

    private String status;

    private String statusName;

    private String creatorName;

    private String assigneeName;

    private Boolean slaBreached;

    private LocalDateTime createTime;
}