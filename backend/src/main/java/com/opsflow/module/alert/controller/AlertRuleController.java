package com.opsflow.module.alert.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.alert.model.dto.AlertRuleDTO;
import com.opsflow.module.alert.model.vo.AlertRuleVO;
import com.opsflow.module.alert.service.AlertRuleService;
import com.opsflow.module.auth.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 告警规则接口
 */
@Tag(name = "告警规则")
@RestController
@RequestMapping("/api/alerts/rules")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('alert:rule:list')")
public class AlertRuleController {

    private final AlertRuleService ruleService;

    @Operation(summary = "分页查询告警规则")
    @GetMapping
    public R<PageResult<AlertRuleVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long hostId) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(ruleService.page(current, size, keyword, status, hostId));
    }

    @Operation(summary = "告警规则详情")
    @GetMapping("/{id}")
    public R<AlertRuleVO> detail(@PathVariable Long id) {
        return R.ok(ruleService.detail(id));
    }

    @Operation(summary = "创建告警规则")
    @PostMapping
    public R<Void> create(@Valid @RequestBody AlertRuleDTO dto) {
        ruleService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改告警规则")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AlertRuleDTO dto) {
        ruleService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除告警规则")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启停用告警规则")
    @PostMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        ruleService.changeStatus(id, body.get("status"), SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "状态已更新");
    }
}