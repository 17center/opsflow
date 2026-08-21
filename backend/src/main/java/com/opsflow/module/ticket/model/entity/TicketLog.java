package com.opsflow.module.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单状态变更日志表实体（ticket_log 表）
 */
@Data
@TableName("ticket_log")
public class TicketLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工单 ID */
    private Long ticketId;

    /** 操作类型：CREATE/SUBMIT/APPROVE/REJECT/ASSIGN/EXECUTE/RESOLVE/CLOSE/REOPEN/COMMENT */
    private String action;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人 ID */
    private Long operatorId;

    /** 操作内容（如审批意见、驳回理由） */
    private String content;

    /** 操作时间 */
    private LocalDateTime createTime;
}