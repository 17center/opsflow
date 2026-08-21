package com.opsflow.module.workflow.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建/更新流程定义请求
 */
@Data
@Schema(description = "创建/更新流程定义请求")
public class WfDefinitionDTO {

    @Schema(description = "流程名称")
    @NotBlank(message = "流程名称不能为空")
    @Size(max = 128, message = "流程名称长度不能超过 128")
    private String name;

    @Schema(description = "流程标识（如 change_approval）")
    @NotBlank(message = "流程标识不能为空")
    @Size(max = 64, message = "流程标识长度不能超过 64")
    private String key;

    @Schema(description = "流程描述")
    private String description;

    @Schema(description = "审批节点列表（按顺序执行）")
    private List<WfNodeDTO> nodes;
}