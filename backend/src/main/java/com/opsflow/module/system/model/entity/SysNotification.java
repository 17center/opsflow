package com.opsflow.module.system.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内通知表实体（sys_notification 表）
 */
@Data
@TableName("sys_notification")
public class SysNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：1=工单 2=审批 3=告警 4=系统 */
    private Integer notifyType;

    /** 关联业务 ID */
    private Long relatedId;

    /** 关联业务类型（TICKET/ALERT） */
    private String relatedType;

    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}