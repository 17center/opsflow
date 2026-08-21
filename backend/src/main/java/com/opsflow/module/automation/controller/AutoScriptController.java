package com.opsflow.module.automation.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.automation.model.dto.AutoScriptDTO;
import com.opsflow.module.automation.model.vo.AutoScriptVO;
import com.opsflow.module.automation.model.vo.AutoScriptVersionVO;
import com.opsflow.module.automation.service.AutoScriptService;
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

import java.util.List;

/**
 * 自动化脚本接口
 */
@Tag(name = "自动化脚本")
@RestController
@RequestMapping("/api/automation/scripts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('auto:script:list')")
public class AutoScriptController {

    private final AutoScriptService autoScriptService;

    @Operation(summary = "分页查询脚本")
    @GetMapping
    public R<PageResult<AutoScriptVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer scriptType,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(autoScriptService.page(current, size, keyword, scriptType, status));
    }

    @Operation(summary = "脚本详情")
    @GetMapping("/{id}")
    public R<AutoScriptVO> detail(@PathVariable Long id) {
        return R.ok(autoScriptService.detail(id));
    }

    @Operation(summary = "创建脚本")
    @PostMapping
    public R<Void> create(@Valid @RequestBody AutoScriptDTO dto) {
        autoScriptService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改脚本（自动生成新版本）")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AutoScriptDTO dto) {
        autoScriptService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除脚本")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        autoScriptService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启用脚本")
    @PostMapping("/{id}/enable")
    public R<Void> enable(@PathVariable Long id) {
        autoScriptService.changeStatus(id, 1, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "已启用");
    }

    @Operation(summary = "停用脚本")
    @PostMapping("/{id}/disable")
    public R<Void> disable(@PathVariable Long id) {
        autoScriptService.changeStatus(id, 0, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "已停用");
    }

    @Operation(summary = "脚本版本列表")
    @GetMapping("/{id}/versions")
    public R<List<AutoScriptVersionVO>> versions(@PathVariable Long id) {
        return R.ok(autoScriptService.versions(id));
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{id}/rollback/{version}")
    public R<Void> rollback(@PathVariable Long id, @PathVariable Integer version) {
        autoScriptService.rollback(id, version, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "回滚成功");
    }
}