package com.opsflow.module.ticket.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解决工单请求
 */
@Data
@Schema(description = "解决工单请求")
public class TicketResolveDTO {

    @Schema(description = "解决方案说明")
    @NotBlank(message = "解决方案不能为空")
    private String resolution;
}