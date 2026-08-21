package com.opsflow.module.automation.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.automation.model.dto.AutoExecStartDTO;
import com.opsflow.module.automation.model.vo.AutoExecDetailVO;
import com.opsflow.module.automation.model.vo.AutoExecRecordVO;
import com.opsflow.module.automation.service.ExecutionService;
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
 * 自动化执行接口
 */
@Tag(name = "自动化执行")
@RestController
@RequestMapping("/api/automation/exec")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('auto:exec:list')")
public class AutoExecController {

    private final ExecutionService executionService;

    @Operation(summary = "触发脚本执行（异步）")
    @PostMapping("/start")
    public R<Map<String, Long>> start(@Valid @RequestBody AutoExecStartDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long id = executionService.start(dto, user.getUserId(), user.getUsername());
        return R.ok(Map.of("id", id), "已提交执行");
    }

    @Operation(summary = "执行记录分页")
    @GetMapping
    public R<PageResult<AutoExecRecordVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long scriptId,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(executionService.page(current, size, scriptId, hostId, status));
    }

    @Operation(summary = "执行详情（含输出日志）")
    @GetMapping("/{id}")
    public R<AutoExecDetailVO> detail(@PathVariable Long id) {
        return R.ok(executionService.detail(id));
    }

    @Operation(summary = "取消执行")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        executionService.cancel(id, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "已取消");
    }
}