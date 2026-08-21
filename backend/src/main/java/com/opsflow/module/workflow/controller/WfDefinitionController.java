package com.opsflow.module.workflow.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.workflow.model.dto.WfDefinitionDTO;
import com.opsflow.module.workflow.model.vo.WfDefinitionVO;
import com.opsflow.module.workflow.service.WfDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程定义接口
 */
@Tag(name = "流程定义")
@RestController
@RequestMapping("/api/workflow/definitions")
@RequiredArgsConstructor
public class WfDefinitionController {

    private final WfDefinitionService wfDefinitionService;

    @Operation(summary = "分页查询流程定义")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @GetMapping
    public R<PageResult<WfDefinitionVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(wfDefinitionService.page(current, size, key, name, status));
    }

    @Operation(summary = "流程定义详情")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @GetMapping("/{id}")
    public R<WfDefinitionVO> detail(@PathVariable Long id) {
        return R.ok(wfDefinitionService.detail(id));
    }

    @Operation(summary = "创建流程定义")
    @AuditLog(module = "WORKFLOW", operation = "创建流程定义")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @PostMapping
    public R<Void> create(@Valid @RequestBody WfDefinitionDTO dto) {
        wfDefinitionService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改流程定义")
    @AuditLog(module = "WORKFLOW", operation = "修改流程定义")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody WfDefinitionDTO dto) {
        wfDefinitionService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "发布流程定义")
    @AuditLog(module = "WORKFLOW", operation = "发布流程定义")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        wfDefinitionService.publish(id, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "发布成功");
    }

    @Operation(summary = "停用流程定义")
    @AuditLog(module = "WORKFLOW", operation = "停用流程定义")
    @PreAuthorize("hasAuthority('workflow:definition:list')")
    @PostMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        wfDefinitionService.disable(id, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "停用成功");
    }
}