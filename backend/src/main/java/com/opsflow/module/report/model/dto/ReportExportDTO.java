package com.opsflow.module.report.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报表导出请求
 */
@Data
@Schema(description = "报表导出请求")
public class ReportExportDTO {

    @Schema(description = "报表类型：ticket_stats=工单统计、sla_compliance=SLA达标、auto_exec=自动化执行、personal_workload=个人工作量")
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    @Schema(description = "导出格式：EXCEL / PDF")
    @NotBlank(message = "导出格式不能为空")
    private String format;

    @Schema(description = "开始日期 yyyy-MM-dd")
    private String startTime;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private String endTime;
}