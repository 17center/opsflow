package com.opsflow.module.report.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.report.model.dto.ReportExportDTO;
import com.opsflow.module.report.model.vo.ReportDashboardVO;
import com.opsflow.module.report.model.vo.ReportExportVO;
import com.opsflow.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 数据统计与报表接口
 */
@Tag(name = "数据报表")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('report:dashboard')")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "运维仪表盘数据")
    @GetMapping("/dashboard")
    public R<ReportDashboardVO> dashboard(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(reportService.dashboard(startTime, endTime));
    }

    @Operation(summary = "报表导出")
    @PostMapping("/export")
    public R<ReportExportVO> export(@Valid @RequestBody ReportExportDTO dto) {
        ReportExportVO vo = new ReportExportVO();
        vo.setDownloadUrl(reportService.export(dto));
        return R.ok(vo);
    }

    @Operation(summary = "下载报表文件")
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        Path path = reportService.resolveReportFile(fileName);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        Files.copy(path, response.getOutputStream());
        response.getOutputStream().flush();
    }
}