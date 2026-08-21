package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单附件 VO
 */
@Data
public class AttachmentVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String filePath;

    private LocalDateTime uploadTime;
}