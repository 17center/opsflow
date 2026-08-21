package com.opsflow.module.knowledge.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标签请求
 */
@Data
@Schema(description = "标签请求")
public class KbTagDTO {

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 32, message = "标签名称长度不能超过 32")
    private String name;
}