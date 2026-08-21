package com.opsflow.module.ticket.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 添加工单评论请求
 */
@Data
@Schema(description = "添加工单评论请求")
public class TicketCommentDTO {

    @Schema(description = "评论内容（Markdown）")
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @Schema(description = "被 @ 的用户 ID 列表")
    private List<Long> mentionedUserIds;
}