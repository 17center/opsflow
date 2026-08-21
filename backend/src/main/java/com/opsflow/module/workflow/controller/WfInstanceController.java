package com.opsflow.module.workflow.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.workflow.model.dto.WfInstanceStartDTO;
import com.opsflow.module.workflow.model.vo.WfInstanceVO;
import com.opsflow.module.workflow.service.WorkflowService;
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

import java.util.Map;

/**
 * 流程实例接口
 */
@Tag(name = "流程实例")
@RestController
@RequestMapping("/api/workflow/instances")
@RequiredArgsConstructor
public class WfInstanceController {

    private final WorkflowService workflowService;

    @Operation(summary = "启动流程实例")
    @AuditLog(module = "WORKFLOW", operation = "启动流程")
    @PostMapping
    public R<Map<String, Long>> start(@Valid @RequestBody WfInstanceStartDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long instanceId = workflowService.start(dto.getTicketId(), dto.getDefinitionId(),
                user.getUserId(), user.getUsername());
        return R.ok(Map.of("id", instanceId), "流程已启动");
    }

    @Operation(summary = "流程实例列表")
    @PreAuthorize("hasAuthority('workflow:instance:list')")
    @GetMapping
    public R<PageResult<WfInstanceVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long ticketId,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(workflowService.pageInstances(current, size, ticketId, status));
    }

    @Operation(summary = "流程实例详情")
    @PreAuthorize("hasAuthority('workflow:instance:list')")
    @GetMapping("/{id}")
    public R<WfInstanceVO> detail(@PathVariable Long id) {
        return R.ok(workflowService.instanceDetail(id));
    }
}