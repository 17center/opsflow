package com.opsflow.module.ticket.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 指派工单请求
 */
@Data
@Schema(description = "指派工单请求")
public class TicketAssignDTO {

    @Schema(description = "处理人 ID")
    @NotNull(message = "处理人不能为空")
    private Long assigneeId;
}