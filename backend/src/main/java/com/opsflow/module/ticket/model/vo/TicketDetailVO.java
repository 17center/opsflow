package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工单详情 VO
 */
@Data
public class TicketDetailVO {

    private Long id;

    private String ticketNo;

    private String title;

    private String description;

    private Integer ticketType;

    private String ticketTypeName;

    private Integer priority;

    private String priorityName;

    private String status;

    private String statusName;

    private UserRefVO creator;

    private UserRefVO assignee;

    private Long hostId;

    private Long scriptId;

    private Map<String, Object> scriptParams;

    private Long wfInstanceId;

    private LocalDateTime slaDeadline;

    private LocalDateTime slaResponseDeadline;

    private Boolean slaBreached;

    private List<CommentVO> comments;

    private List<AttachmentVO> attachments;

    private List<LogVO> logs;

    private LocalDateTime createTime;
}