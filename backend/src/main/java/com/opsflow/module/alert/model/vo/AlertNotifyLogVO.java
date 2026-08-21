package com.opsflow.module.alert.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警通知记录 VO
 */
@Data
public class AlertNotifyLogVO {

    private Long id;

    private Long eventId;

    /** 通知渠道 */
    private String channel;

    /** 接收人 */
    private String receiver;

    /** 发送状态：1=成功 2=失败 */
    private Integer status;

    private String statusName;

    /** 失败原因 */
    private String errorMessage;

    private LocalDateTime createTime;
}