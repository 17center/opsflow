package com.opsflow.module.ticket.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.ticket.model.dto.TicketAssignDTO;
import com.opsflow.module.ticket.model.dto.TicketCreateDTO;
import com.opsflow.module.ticket.model.dto.TicketReopenDTO;
import com.opsflow.module.ticket.model.dto.TicketResolveDTO;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.vo.DashboardVO;
import com.opsflow.module.ticket.model.vo.TicketDetailVO;
import com.opsflow.module.ticket.model.vo.TicketVO;
import com.opsflow.module.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 工单管理接口
 */
@Tag(name = "工单管理")
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "分页查询工单")
    @PreAuthorize("hasAuthority('ticket:list')")
    @GetMapping
    public R<PageResult<TicketVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Integer ticketType,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(ticketService.page(current, size, keyword, status, priority, ticketType,
                creatorId, assigneeId, startTime, endTime));
    }

    @Operation(summary = "工单详情")
    @PreAuthorize("hasAuthority('ticket:list')")
    @GetMapping("/{id}")
    public R<TicketDetailVO> detail(@PathVariable Long id) {
        return R.ok(ticketService.detail(id));
    }

    @Operation(summary = "创建工单")
    @AuditLog(module = "TICKET", operation = "创建工单")
    @PreAuthorize("hasAuthority('ticket:create')")
    @PostMapping
    public R<Ticket> create(@Valid @RequestBody TicketCreateDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        return R.ok(ticketService.create(dto, user.getUserId(), user.getUsername()), "创建成功");
    }

    @Operation(summary = "提交工单")
    @AuditLog(module = "TICKET", operation = "提交工单")
    @PreAuthorize("hasAuthority('ticket:create')")
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        ticketService.submit(id, user.getUserId(), user.getUsername());
        return R.ok(null, "提交成功");
    }

    @Operation(summary = "指派工单")
    @AuditLog(module = "TICKET", operation = "指派工单")
    @PostMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @Valid @RequestBody TicketAssignDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        ticketService.assign(id, dto.getAssigneeId(), user.getUserId(), user.getUsername());
        return R.ok(null, "指派成功");
    }

    @Operation(summary = "解决工单")
    @AuditLog(module = "TICKET", operation = "解决工单")
    @PostMapping("/{id}/resolve")
    public R<Void> resolve(@PathVariable Long id, @Valid @RequestBody TicketResolveDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        ticketService.resolve(id, dto.getResolution(), user.getUserId(), user.getUsername());
        return R.ok(null, "解决成功");
    }

    @Operation(summary = "关闭工单")
    @AuditLog(module = "TICKET", operation = "关闭工单")
    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        ticketService.close(id, user.getUserId(), user.getUsername());
        return R.ok(null, "关闭成功");
    }

    @Operation(summary = "重新打开工单")
    @AuditLog(module = "TICKET", operation = "重新打开工单")
    @PostMapping("/{id}/reopen")
    public R<Void> reopen(@PathVariable Long id, @Valid @RequestBody TicketReopenDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        ticketService.reopen(id, dto.getReason(), user.getUserId(), user.getUsername());
        return R.ok(null, "重新打开成功");
    }

    @Operation(summary = "工单看板统计")
    @PreAuthorize("hasAuthority('ticket:list')")
    @GetMapping("/dashboard")
    public R<DashboardVO> dashboard() {
        return R.ok(ticketService.dashboard());
    }
}