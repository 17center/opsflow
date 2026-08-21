package com.opsflow.module.workflow.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流程节点配置（简化 BPMN，线性审批节点）
 */
@Data
@Schema(description = "流程节点配置")
public class WfNodeDTO {

    @Schema(description = "节点标识，如 n1")
    @NotBlank(message = "节点标识不能为空")
    private String nodeKey;

    @Schema(description = "节点名称")
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    @Schema(description = "节点类型：1=人工审批 2=自动执行 3=通知")
    private Integer nodeType = 1;

    @Schema(description = "指定审批人 ID（与 candidateGroup 二选一）")
    private Long assigneeId;

    @Schema(description = "候选审批角色编码")
    private String candidateGroup;

    @Schema(description = "签批方式：1=单人 2=会签 3=或签")
    private Integer signType = 1;
}