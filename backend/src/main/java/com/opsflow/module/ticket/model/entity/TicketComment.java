package com.opsflow.module.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单评论表实体（ticket_comment 表）
 */
@Data
@TableName("ticket_comment")
public class TicketComment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工单 ID */
    private Long ticketId;

    /** 评论人 ID */
    private Long userId;

    /** 评论内容（Markdown） */
    private String content;

    /** 被 @ 的用户 ID 列表，逗号分隔 */
    private String mentionedUserIds;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}