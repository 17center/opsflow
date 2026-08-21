package com.opsflow.module.workflow.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转交审批请求
 */
@Data
@Schema(description = "转交审批请求")
public class WfTaskDelegateDTO {

    @Schema(description = "目标审批人 ID")
    @NotNull(message = "目标审批人不能为空")
    private Long targetUserId;

    @Schema(description = "转交说明")
    private String comment;
}