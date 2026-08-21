package com.opsflow.module.ticket.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 创建工单请求
 */
@Data
@Schema(description = "创建工单请求")
public class TicketCreateDTO {

    @Schema(description = "工单标题")
    @NotBlank(message = "工单标题不能为空")
    @Size(max = 256, message = "工单标题长度不能超过 256")
    private String title;

    @Schema(description = "工单描述（支持 Markdown）")
    private String description;

    @Schema(description = "工单类型：1=变更 2=故障 3=请求 4=巡检")
    @NotNull(message = "工单类型不能为空")
    private Integer ticketType;

    @Schema(description = "优先级：0=紧急 1=高 2=中 3=低")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @Schema(description = "关联的表单模板 ID")
    private Long templateId;

    @Schema(description = "关联的目标主机 ID")
    private Long hostId;

    @Schema(description = "关联的自动化脚本 ID")
    private Long scriptId;

    @Schema(description = "脚本执行参数（JSON）")
    private Map<String, Object> scriptParams;
}