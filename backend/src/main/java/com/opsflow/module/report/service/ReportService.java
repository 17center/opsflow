package com.opsflow.module.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.alert.mapper.AlertEventMapper;
import com.opsflow.module.alert.model.entity.AlertEvent;
import com.opsflow.module.automation.mapper.AutoExecRecordMapper;
import com.opsflow.module.automation.mapper.AutoScriptMapper;
import com.opsflow.module.automation.model.entity.AutoExecRecord;
import com.opsflow.module.automation.model.entity.AutoScript;
import com.opsflow.module.report.model.dto.ReportExportDTO;
import com.opsflow.module.report.model.vo.ReportDashboardVO;
import com.opsflow.module.ticket.enums.TicketPriority;
import com.opsflow.module.ticket.enums.TicketStatus;
import com.opsflow.module.ticket.enums.TicketType;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据统计与报表服务：
 * 1. 运维仪表盘 KPI 统计
 * 2. 报表生成（Excel/PDF）并落盘，供下载端点读取
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketMapper ticketMapper;
    private final AutoExecRecordMapper execRecordMapper;
    private final AutoScriptMapper scriptMapper;
    private final AlertEventMapper alertEventMapper;

    /** 报表文件存放目录 */
    private static final String REPORT_DIR = System.getProperty("java.io.tmpdir", "/tmp")
            + File.separator + "opsflow-reports";

    // ==================== 仪表盘 ====================

    public ReportDashboardVO dashboard(String start, String end) {
        LocalDateTime startDt = parseStart(start);
        LocalDateTime endDt = parseEnd(end, startDt);

        ReportDashboardVO vo = new ReportDashboardVO();
        vo.setTicketSummary(buildTicketSummary(startDt, endDt));
        vo.setSlaCompliance(buildSlaCompliance(startDt, endDt));
        vo.setAutoExec(buildAutoExec(startDt, endDt));
        vo.setAlertSummary(buildAlertSummary(startDt, endDt));
        vo.setTicketTrend(buildTicketTrend(startDt, endDt));
        return vo;
    }

    private ReportDashboardVO.TicketSummary buildTicketSummary(LocalDateTime start, LocalDateTime end) {
        ReportDashboardVO.TicketSummary s = new ReportDashboardVO.TicketSummary();
        s.setTotal(ticketMapper.selectCount(null));
        s.setCreated(ticketMapper.selectCount(Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end)));
        s.setResolved(ticketMapper.selectCount(Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end)));
        s.setClosed(ticketMapper.selectCount(Wrappers.<Ticket>lambdaQuery().ge(Ticket::getClosedTime, start).lt(Ticket::getClosedTime, end)));

        // 平均解决时长（对区间内已解决工单）
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        if (resolved.isEmpty()) {
            s.setAvgMttrHours(0D);
        } else {
            double sum = 0;
            for (Ticket t : resolved) {
                if (t.getCreateTime() != null && t.getResolvedTime() != null) {
                    sum += Duration.between(t.getCreateTime(), t.getResolvedTime()).toMillis() / 3600000.0;
                }
            }
            s.setAvgMttrHours(Math.round(sum / resolved.size() * 100.0) / 100.0);
        }
        return s;
    }

    private ReportDashboardVO.SlaCompliance buildSlaCompliance(LocalDateTime start, LocalDateTime end) {
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        ReportDashboardVO.SlaCompliance sc = new ReportDashboardVO.SlaCompliance();
        sc.setTotal((long) resolved.size());
        long breached = 0;
        for (Ticket t : resolved) {
            if (t.getSlaDeadline() != null && t.getResolvedTime() != null && t.getResolvedTime().isAfter(t.getSlaDeadline())) {
                breached++;
            }
        }
        sc.setBreached(breached);
        sc.setComplianceRate(resolved.isEmpty() ? 0D : Math.round((resolved.size() - breached) * 10000.0 / resolved.size()) / 100.0);
        return sc;
    }

    private ReportDashboardVO.AutoExec buildAutoExec(LocalDateTime start, LocalDateTime end) {
        List<AutoExecRecord> records = execRecordMapper.selectList(
                Wrappers.<AutoExecRecord>lambdaQuery().ge(AutoExecRecord::getCreateTime, start).lt(AutoExecRecord::getCreateTime, end));
        ReportDashboardVO.AutoExec ae = new ReportDashboardVO.AutoExec();
        ae.setTotal((long) records.size());
        ae.setSuccess(records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 3).count());
        ae.setFailed(records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 4).count());
        ae.setTimeout(records.stream().filter(r -> r.getStatus() != null && r.getStatus() == 5).count());
        long finished = records.stream().filter(r -> r.getStatus() != null && (r.getStatus() == 3 || r.getStatus() == 4 || r.getStatus() == 5)).count();
        ae.setSuccessRate(finished == 0 ? 0D : Math.round(ae.getSuccess() * 10000.0 / finished) / 100.0);
        return ae;
    }

    private ReportDashboardVO.AlertSummary buildAlertSummary(LocalDateTime start, LocalDateTime end) {
        List<AlertEvent> events = alertEventMapper.selectList(
                Wrappers.<AlertEvent>lambdaQuery().ge(AlertEvent::getCreateTime, start).lt(AlertEvent::getCreateTime, end));
        ReportDashboardVO.AlertSummary as = new ReportDashboardVO.AlertSummary();
        as.setTotal((long) events.size());
        as.setActive(events.stream().filter(e -> e.getStatus() != null && (e.getStatus() == 1 || e.getStatus() == 2)).count());
        as.setResolved(events.stream().filter(e -> e.getStatus() != null && e.getStatus() == 3).count());

        List<AlertEvent> recovered = events.stream()
                .filter(e -> e.getStatus() != null && e.getStatus() == 3
                        && e.getCreateTime() != null && e.getRecoverTime() != null)
                .toList();
        if (recovered.isEmpty()) {
            as.setAvgResolveMinutes(0D);
        } else {
            double sum = 0;
            for (AlertEvent e : recovered) {
                sum += Duration.between(e.getCreateTime(), e.getRecoverTime()).toMinutes();
            }
            as.setAvgResolveMinutes(Math.round(sum / recovered.size() * 100.0) / 100.0);
        }
        return as;
    }

    private List<ReportDashboardVO.TrendItem> buildTicketTrend(LocalDateTime start, LocalDateTime end) {
        List<Ticket> tickets = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end));
        // 额外补充区间内被解决但创建时间在区间外的工单？保持简单：仅统计创建时间在区间内的工单，解决数按 resolved_time 统计
        List<Ticket> resolvedAll = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));

        LocalDate d = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        List<ReportDashboardVO.TrendItem> items = new ArrayList<>();
        while (!d.isAfter(endDate)) {
            String day = d.format(DATE_FMT);
            final LocalDate dayRef = d;
            long created = tickets.stream()
                    .filter(t -> t.getCreateTime() != null && t.getCreateTime().toLocalDate().equals(dayRef)).count();
            long resolved = resolvedAll.stream()
                    .filter(t -> t.getResolvedTime() != null && t.getResolvedTime().toLocalDate().equals(dayRef)).count();
            ReportDashboardVO.TrendItem item = new ReportDashboardVO.TrendItem();
            item.setDate(day);
            item.setCreated(created);
            item.setResolved(resolved);
            items.add(item);
            d = d.plusDays(1);
        }
        return items;
    }

    // ==================== 报表导出 ====================

    public String export(ReportExportDTO dto) {
        String reportType = dto.getReportType();
        String format = dto.getFormat();
        if (!"ticket_stats".equals(reportType) && !"sla_compliance".equals(reportType)
                && !"auto_exec".equals(reportType) && !"personal_workload".equals(reportType)) {
            throw new BusinessException(ErrorCode.REPORT_TYPE_INVALID);
        }
        if (!"EXCEL".equalsIgnoreCase(format) && !"PDF".equalsIgnoreCase(format)) {
            throw new BusinessException(ErrorCode.REPORT_FORMAT_INVALID);
        }
        LocalDateTime startDt = parseStart(dto.getStartTime());
        LocalDateTime endDt = parseEnd(dto.getEndTime(), startDt);

        String fileName;
        try {
            Files.createDirectories(Paths.get(REPORT_DIR));
            if ("EXCEL".equalsIgnoreCase(format)) {
                byte[] bytes = buildExcel(reportType, startDt, endDt);
                fileName = "report_" + reportType + "_" + LocalDate.now().format(DATE_FMT) + "_" + UUID.randomUUID().toString().substring(0, 8) + ".xlsx";
                writeFile(fileName, bytes);
            } else {
                byte[] bytes = buildPdf(reportType, startDt, endDt);
                fileName = "report_" + reportType + "_" + LocalDate.now().format(DATE_FMT) + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
                writeFile(fileName, bytes);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
        return "/api/reports/download/" + fileName;
    }

    public Path resolveReportFile(String fileName) {
        Path path = Paths.get(REPORT_DIR).resolve(fileName).normalize();
        if (!path.startsWith(Paths.get(REPORT_DIR).toAbsolutePath()) || !Files.exists(path)) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return path;
    }

    private void writeFile(String fileName, byte[] bytes) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(new File(REPORT_DIR, fileName))) {
            fos.write(bytes);
        }
    }

    // ==================== Excel 生成 ====================

    private byte[] buildExcel(String reportType, LocalDateTime start, LocalDateTime end) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle header = wb.createCellStyle();
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
            headerFont.setBold(true);
            header.setFont(headerFont);

            switch (reportType) {
                case "ticket_stats" -> fillTicketStatsSheet(wb, header, start, end);
                case "sla_compliance" -> fillSlaSheet(wb, header, start, end);
                case "auto_exec" -> fillAutoExecSheet(wb, header, start, end);
                case "personal_workload" -> fillWorkloadSheet(wb, header, start, end);
                default -> throw new BusinessException(ErrorCode.REPORT_TYPE_INVALID);
            }
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    private void fillTicketStatsSheet(Workbook wb, CellStyle header, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = wb.createSheet("工单统计");
        List<Ticket> tickets = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end));

        String[] title = {"工单号", "标题", "类型", "优先级", "状态", "创建时间", "解决时间"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < title.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(title[i]);
            c.setCellStyle(header);
        }
        int r = 1;
        for (Ticket t : tickets) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(t.getTicketNo());
            row.createCell(1).setCellValue(t.getTitle());
            row.createCell(2).setCellValue(TicketType.nameOf(t.getTicketType()));
            row.createCell(3).setCellValue(TicketPriority.nameOf(t.getPriority()));
            row.createCell(4).setCellValue(TicketStatus.nameOf(t.getStatus()));
            row.createCell(5).setCellValue(t.getCreateTime() == null ? "" : t.getCreateTime().format(DT_FMT));
            row.createCell(6).setCellValue(t.getResolvedTime() == null ? "" : t.getResolvedTime().format(DT_FMT));
        }
        for (int i = 0; i < title.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void fillSlaSheet(Workbook wb, CellStyle header, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = wb.createSheet("SLA 达标");
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        String[] title = {"工单号", "标题", "优先级", "创建时间", "解决时间", "SLA截止", "是否达标"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < title.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(title[i]);
            c.setCellStyle(header);
        }
        int r = 1;
        for (Ticket t : resolved) {
            boolean ok = t.getSlaDeadline() == null || t.getResolvedTime() == null || !t.getResolvedTime().isAfter(t.getSlaDeadline());
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(t.getTicketNo());
            row.createCell(1).setCellValue(t.getTitle());
            row.createCell(2).setCellValue(TicketPriority.nameOf(t.getPriority()));
            row.createCell(3).setCellValue(t.getCreateTime() == null ? "" : t.getCreateTime().format(DT_FMT));
            row.createCell(4).setCellValue(t.getResolvedTime() == null ? "" : t.getResolvedTime().format(DT_FMT));
            row.createCell(5).setCellValue(t.getSlaDeadline() == null ? "" : t.getSlaDeadline().format(DT_FMT));
            row.createCell(6).setCellValue(ok ? "达标" : "未达标");
        }
        for (int i = 0; i < title.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void fillAutoExecSheet(Workbook wb, CellStyle header, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = wb.createSheet("自动化执行");
        List<AutoExecRecord> records = execRecordMapper.selectList(
                Wrappers.<AutoExecRecord>lambdaQuery().ge(AutoExecRecord::getCreateTime, start).lt(AutoExecRecord::getCreateTime, end));
        String[] title = {"ID", "脚本", "目标主机", "触发方式", "状态", "开始时间", "耗时(ms)"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < title.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(title[i]);
            c.setCellStyle(header);
        }
        Map<Long, String> scriptNames = new LinkedHashMap<>();
        List<AutoScript> scripts = scriptMapper.selectList(null);
        for (AutoScript s : scripts) {
            scriptNames.put(s.getId(), s.getName());
        }
        int r = 1;
        for (AutoExecRecord rec : records) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(rec.getId());
            row.createCell(1).setCellValue(scriptNames.getOrDefault(rec.getScriptId(), "脚本#" + rec.getScriptId()));
            row.createCell(2).setCellValue(rec.getHostId() == null ? "" : "主机#" + rec.getHostId());
            row.createCell(3).setCellValue(rec.getTriggerType() != null && rec.getTriggerType() == 1 ? "工单触发" : "手动触发");
            row.createCell(4).setCellValue(execStatusName(rec.getStatus()));
            row.createCell(5).setCellValue(rec.getStartTime() == null ? "" : rec.getStartTime().format(DT_FMT));
            row.createCell(6).setCellValue(rec.getDurationMs() == null ? 0 : rec.getDurationMs());
        }
        for (int i = 0; i < title.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void fillWorkloadSheet(Workbook wb, CellStyle header, LocalDateTime start, LocalDateTime end) {
        Sheet sheet = wb.createSheet("个人工作量");
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        Map<String, long[]> byUser = new LinkedHashMap<>();
        for (Ticket t : resolved) {
            String key = t.getUpdateBy() == null ? "未知" : t.getUpdateBy();
            byUser.computeIfAbsent(key, k -> new long[2])[0]++;
        }
        List<Ticket> created = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end));
        for (Ticket t : created) {
            String key = t.getCreateBy() == null ? "未知" : t.getCreateBy();
            byUser.computeIfAbsent(key, k -> new long[2])[1]++;
        }
        String[] title = {"操作人", "解决工单数", "创建工单数", "合计"};
        Row h = sheet.createRow(0);
        for (int i = 0; i < title.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(title[i]);
            c.setCellStyle(header);
        }
        int r = 1;
        for (Map.Entry<String, long[]> e : byUser.entrySet()) {
            Row row = sheet.createRow(r++);
            long[] v = e.getValue();
            row.createCell(0).setCellValue(e.getKey());
            row.createCell(1).setCellValue(v[0]);
            row.createCell(2).setCellValue(v[1]);
            row.createCell(3).setCellValue(v[0] + v[1]);
        }
        for (int i = 0; i < title.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ==================== PDF 生成 ====================

    private byte[] buildPdf(String reportType, LocalDateTime start, LocalDateTime end) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            switch (reportType) {
                case "ticket_stats" -> pdfTicketStats(doc, start, end);
                case "sla_compliance" -> pdfSla(doc, start, end);
                case "auto_exec" -> pdfAutoExec(doc, start, end);
                case "personal_workload" -> pdfWorkload(doc, start, end);
                default -> throw new BusinessException(ErrorCode.REPORT_TYPE_INVALID);
            }
            doc.close();
            return out.toByteArray();
        }
    }

    private void pdfHeader(Document doc, String title) throws Exception {
        Font font = new Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD);
        Paragraph p = new Paragraph(title, font);
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        doc.add(new Paragraph("生成时间：" + LocalDateTime.now().format(DT_FMT)));
        doc.add(new Paragraph(" "));
    }

    private void pdfTicketStats(Document doc, LocalDateTime start, LocalDateTime end) throws Exception {
        pdfHeader(doc, "工单统计报表");
        List<Ticket> tickets = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end));
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        for (String h : new String[]{"工单号", "标题", "类型", "优先级", "状态"}) {
            table.addCell(h);
        }
        for (Ticket t : tickets) {
            table.addCell(t.getTicketNo());
            table.addCell(t.getTitle());
            table.addCell(TicketType.nameOf(t.getTicketType()));
            table.addCell(TicketPriority.nameOf(t.getPriority()));
            table.addCell(TicketStatus.nameOf(t.getStatus()));
        }
        doc.add(table);
    }

    private void pdfSla(Document doc, LocalDateTime start, LocalDateTime end) throws Exception {
        pdfHeader(doc, "SLA 达标报表");
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        for (String h : new String[]{"工单号", "标题", "优先级", "是否达标"}) {
            table.addCell(h);
        }
        for (Ticket t : resolved) {
            boolean ok = t.getSlaDeadline() == null || t.getResolvedTime() == null || !t.getResolvedTime().isAfter(t.getSlaDeadline());
            table.addCell(t.getTicketNo());
            table.addCell(t.getTitle());
            table.addCell(TicketPriority.nameOf(t.getPriority()));
            table.addCell(ok ? "达标" : "未达标");
        }
        doc.add(table);
    }

    private void pdfAutoExec(Document doc, LocalDateTime start, LocalDateTime end) throws Exception {
        pdfHeader(doc, "自动化执行报表");
        List<AutoExecRecord> records = execRecordMapper.selectList(
                Wrappers.<AutoExecRecord>lambdaQuery().ge(AutoExecRecord::getCreateTime, start).lt(AutoExecRecord::getCreateTime, end));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        for (String h : new String[]{"ID", "脚本", "状态", "耗时(ms)"}) {
            table.addCell(h);
        }
        Map<Long, String> scriptNames = new LinkedHashMap<>();
        for (AutoScript s : scriptMapper.selectList(null)) {
            scriptNames.put(s.getId(), s.getName());
        }
        for (AutoExecRecord rec : records) {
            table.addCell(String.valueOf(rec.getId()));
            table.addCell(scriptNames.getOrDefault(rec.getScriptId(), "脚本#" + rec.getScriptId()));
            table.addCell(execStatusName(rec.getStatus()));
            table.addCell(rec.getDurationMs() == null ? "0" : String.valueOf(rec.getDurationMs()));
        }
        doc.add(table);
    }

    private void pdfWorkload(Document doc, LocalDateTime start, LocalDateTime end) throws Exception {
        pdfHeader(doc, "个人工作量报表");
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getResolvedTime, start).lt(Ticket::getResolvedTime, end));
        Map<String, long[]> byUser = new LinkedHashMap<>();
        for (Ticket t : resolved) {
            String key = t.getUpdateBy() == null ? "未知" : t.getUpdateBy();
            byUser.computeIfAbsent(key, k -> new long[2])[0]++;
        }
        List<Ticket> created = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery().ge(Ticket::getCreateTime, start).lt(Ticket::getCreateTime, end));
        for (Ticket t : created) {
            String key = t.getCreateBy() == null ? "未知" : t.getCreateBy();
            byUser.computeIfAbsent(key, k -> new long[2])[1]++;
        }
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        for (String h : new String[]{"操作人", "解决工单数", "创建工单数", "合计"}) {
            table.addCell(h);
        }
        for (Map.Entry<String, long[]> e : byUser.entrySet()) {
            table.addCell(e.getKey());
            table.addCell(String.valueOf(e.getValue()[0]));
            table.addCell(String.valueOf(e.getValue()[1]));
            table.addCell(String.valueOf(e.getValue()[0] + e.getValue()[1]));
        }
        doc.add(table);
    }

    private String execStatusName(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "等待";
            case 2 -> "执行中";
            case 3 -> "成功";
            case 4 -> "失败";
            case 5 -> "超时";
            case 6 -> "取消";
            default -> "未知";
        };
    }

    // ==================== 时间工具 ====================

    private LocalDateTime parseStart(String time) {
        if (time == null || time.isBlank()) {
            return LocalDate.now().minusDays(29).atStartOfDay();
        }
        return LocalDate.parse(time, DATE_FMT).atStartOfDay();
    }

    private LocalDateTime parseEnd(String time, LocalDateTime start) {
        LocalDateTime end;
        if (time == null || time.isBlank()) {
            end = LocalDate.now().plusDays(1).atStartOfDay();
        } else {
            end = LocalDate.parse(time, DATE_FMT).plusDays(1).atStartOfDay();
        }
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.REPORT_TIME_RANGE_INVALID);
        }
        return end;
    }
}