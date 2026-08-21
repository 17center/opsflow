package com.opsflow.module.knowledge.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 智能问答请求
 */
@Data
@Schema(description = "智能问答请求")
public class KbQaDTO {

    @Schema(description = "问题")
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过 500")
    private String question;

    @Schema(description = "会话 ID（用于历史对话，可选）")
    private String conversationId;
}