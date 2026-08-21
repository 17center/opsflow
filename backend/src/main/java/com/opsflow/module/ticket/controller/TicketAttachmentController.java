package com.opsflow.module.ticket.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.ticket.model.vo.AttachmentVO;
import com.opsflow.module.ticket.service.TicketAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 工单附件接口
 */
@Tag(name = "工单附件")
@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService attachmentService;

    @Operation(summary = "上传附件（≤20MB）")
    @AuditLog(module = "TICKET", operation = "上传附件")
    @PostMapping
    public R<AttachmentVO> upload(@PathVariable Long ticketId, @RequestParam("file") MultipartFile file) {
        LoginUser user = SecurityUtils.getLoginUser();
        return R.ok(attachmentService.upload(ticketId, file, user.getUserId(), user.getUsername()), "上传成功");
    }

    @Operation(summary = "下载附件")
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        TicketAttachmentService.DownloadResult result = attachmentService.download(ticketId, attachmentId);
        String encoded = new String(result.fileName().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.content());
    }
}