package com.opsflow.module.automation.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.automation.model.dto.CmdbHostDTO;
import com.opsflow.module.automation.model.vo.CmdbHostVO;
import com.opsflow.module.automation.service.CmdbHostService;
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
 * 目标主机接口
 */
@Tag(name = "目标主机")
@RestController
@RequestMapping("/api/automation/hosts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('cmdb:host:list')")
public class CmdbHostController {

    private final CmdbHostService cmdbHostService;

    @Operation(summary = "分页查询主机")
    @GetMapping
    public R<PageResult<CmdbHostVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String groupName) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(cmdbHostService.page(current, size, keyword, status, groupName));
    }

    @Operation(summary = "主机详情")
    @GetMapping("/{id}")
    public R<CmdbHostVO> detail(@PathVariable Long id) {
        return R.ok(cmdbHostService.detail(id));
    }

    @Operation(summary = "新增主机")
    @PostMapping
    public R<Void> create(@Valid @RequestBody CmdbHostDTO dto) {
        cmdbHostService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改主机")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CmdbHostDTO dto) {
        cmdbHostService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除主机")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cmdbHostService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "连接测试")
    @PostMapping("/{id}/test")
    public R<Map<String, String>> test(@PathVariable Long id) {
        String result = cmdbHostService.testConnection(id);
        return R.ok(Map.of("result", result));
    }
}