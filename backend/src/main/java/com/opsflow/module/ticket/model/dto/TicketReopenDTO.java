package com.opsflow.module.ticket.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 重新打开工单请求
 */
@Data
@Schema(description = "重新打开工单请求")
public class TicketReopenDTO {

    @Schema(description = "重新打开原因")
    @NotBlank(message = "重新打开原因不能为空")
    private String reason;
}