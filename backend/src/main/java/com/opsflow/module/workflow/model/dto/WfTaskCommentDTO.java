package com.opsflow.module.workflow.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批通过/驳回请求
 */
@Data
@Schema(description = "审批操作请求")
public class WfTaskCommentDTO {

    @Schema(description = "审批意见")
    private String comment;
}