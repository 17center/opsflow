package com.opsflow.module.workflow.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启动流程实例请求
 */
@Data
@Schema(description = "启动流程实例请求")
public class WfInstanceStartDTO {

    @Schema(description = "关联工单 ID")
    @NotNull(message = "工单不能为空")
    private Long ticketId;

    @Schema(description = "流程定义 ID")
    @NotNull(message = "流程定义不能为空")
    private Long definitionId;
}