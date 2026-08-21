package com.opsflow.module.ticket.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单评论 VO
 */
@Data
public class CommentVO {

    private Long id;

    private UserRefVO user;

    private String content;

    private LocalDateTime createTime;
}