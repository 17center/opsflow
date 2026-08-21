package com.opsflow.module.workflow.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.workflow.model.dto.WfTaskCommentDTO;
import com.opsflow.module.workflow.model.dto.WfTaskDelegateDTO;
import com.opsflow.module.workflow.model.vo.WfTaskTodoVO;
import com.opsflow.module.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审批任务接口
 */
@Tag(name = "审批任务")
@RestController
@RequestMapping("/api/workflow/tasks")
@RequiredArgsConstructor
public class WfTaskController {

    private final WorkflowService workflowService;

    @Operation(summary = "我的待办任务")
    @GetMapping("/todo")
    public R<PageResult<WfTaskTodoVO>> todo(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        if (size > 100) {
            size = 100;
        }
        LoginUser user = SecurityUtils.getLoginUser();
        return R.ok(workflowService.todo(user.getUserId(), current, size));
    }

    @Operation(summary = "审批通过")
    @AuditLog(module = "WORKFLOW", operation = "审批通过")
    @PostMapping("/{taskId}/approve")
    public R<Void> approve(@PathVariable Long taskId, @Valid @RequestBody WfTaskCommentDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        workflowService.approve(taskId, dto, user.getUserId(), user.getUsername());
        return R.ok(null, "已通过");
    }

    @Operation(summary = "审批驳回")
    @AuditLog(module = "WORKFLOW", operation = "审批驳回")
    @PostMapping("/{taskId}/reject")
    public R<Void> reject(@PathVariable Long taskId, @Valid @RequestBody WfTaskCommentDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        workflowService.reject(taskId, dto, user.getUserId(), user.getUsername());
        return R.ok(null, "已驳回");
    }

    @Operation(summary = "转交审批")
    @AuditLog(module = "WORKFLOW", operation = "转交审批")
    @PostMapping("/{taskId}/delegate")
    public R<Void> delegate(@PathVariable Long taskId, @Valid @RequestBody WfTaskDelegateDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        workflowService.delegate(taskId, dto, user.getUserId(), user.getUsername());
        return R.ok(null, "转交成功");
    }
}