package com.opsflow.module.ticket.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.ticket.model.dto.TicketCommentDTO;
import com.opsflow.module.ticket.model.vo.CommentVO;
import com.opsflow.module.ticket.service.TicketCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单评论接口
 */
@Tag(name = "工单评论")
@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class TicketCommentController {

    private final TicketCommentService commentService;

    @Operation(summary = "发表评论")
    @AuditLog(module = "TICKET", operation = "工单评论")
    @PostMapping
    public R<Void> add(@PathVariable Long ticketId, @Valid @RequestBody TicketCommentDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        commentService.add(ticketId, dto, user.getUserId(), user.getUsername());
        return R.ok(null, "评论成功");
    }

    @Operation(summary = "评论列表")
    @GetMapping
    public R<List<CommentVO>> list(@PathVariable Long ticketId) {
        return R.ok(commentService.list(ticketId));
    }
}