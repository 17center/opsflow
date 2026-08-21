package com.opsflow.module.system.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知 VO
 */
@Data
public class NotificationVO {

    private Long id;

    private String title;

    private String content;

    /** 通知类型：1=工单 2=审批 3=告警 4=系统 */
    private Integer notifyType;

    private Long relatedId;

    private String relatedType;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}