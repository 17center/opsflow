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
 * 工单附件表实体（ticket_attachment 表）
 */
@Data
@TableName("ticket_attachment")
public class TicketAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属工单 ID */
    private Long ticketId;

    /** 所属评论 ID（评论附件时有值） */
    private Long commentId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径（MinIO key） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String fileType;

    /** 上传人 ID */
    private Long uploaderId;

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