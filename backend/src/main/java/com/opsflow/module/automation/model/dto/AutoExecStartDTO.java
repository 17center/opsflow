package com.opsflow.module.automation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启动脚本执行请求
 */
@Data
@Schema(description = "启动脚本执行请求")
public class AutoExecStartDTO {

    @Schema(description = "脚本 ID")
    @NotNull(message = "脚本不能为空")
    private Long scriptId;

    @Schema(description = "目标主机 ID")
    @NotNull(message = "目标主机不能为空")
    private Long hostId;

    @Schema(description = "关联工单 ID（可选）")
    private Long ticketId;

    @Schema(description = "触发方式：1=工单自动触发 2=手动触发，默认 2")
    private Integer triggerType = 2;
}