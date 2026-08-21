package com.opsflow.module.system.controller;

import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.system.model.vo.AuditLogVO;
import com.opsflow.module.system.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 操作审计日志接口（需 sys:audit:list 权限）
 */
@Tag(name = "操作审计日志")
@RestController
@RequestMapping("/api/system/audit-logs")
@RequiredArgsConstructor
public class SysAuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志")
    @PreAuthorize("hasAuthority('sys:audit:list')")
    @GetMapping
    public R<PageResult<AuditLogVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(auditLogService.page(current, size, username, module, status, startTime, endTime));
    }

    @Operation(summary = "清理指定时间之前的历史日志")
    @PreAuthorize("hasAuthority('sys:audit:list')")
    @DeleteMapping("/clean")
    public R<Void> clean(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beforeTime) {
        if (beforeTime == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        auditLogService.cleanBefore(beforeTime);
        return R.ok(null, "清理成功");
    }
}