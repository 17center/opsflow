package com.opsflow.module.alert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警通知记录实体（alert_notify_log 表）
 */
@Data
@TableName("alert_notify_log")
public class AlertNotifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的告警事件 ID */
    private Long eventId;

    /** 通知渠道：EMAIL/FEISHU/DINGTALK/WEBHOOK */
    private String channel;

    /** 接收人（邮箱/用户 ID/Webhook URL） */
    private String receiver;

    /** 发送状态：1=成功 2=失败 */
    private Integer status;

    /** 发送失败原因 */
    private String errorMessage;

    /** 发送时间 */
    private LocalDateTime createTime;
}