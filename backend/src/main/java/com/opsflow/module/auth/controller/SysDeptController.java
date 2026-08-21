package com.opsflow.module.auth.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.model.dto.DeptDTO;
import com.opsflow.module.auth.model.vo.DeptTreeVO;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.auth.service.DeptService;
import com.opsflow.module.system.annotation.AuditLog;
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
 * 部门管理接口（需 sys:dept:manage 权限）
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class SysDeptController {

    private final DeptService deptService;

    @Operation(summary = "查询部门树")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    @GetMapping("/tree")
    public R<List<DeptTreeVO>> tree(@RequestParam(required = false) String deptName,
                                    @RequestParam(required = false) Integer status) {
        return R.ok(deptService.tree(deptName, status));
    }

    @Operation(summary = "新增部门")
    @AuditLog(module = "USER", operation = "新增部门")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    @PostMapping
    public R<Void> create(@Valid @RequestBody DeptDTO dto) {
        deptService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改部门")
    @AuditLog(module = "USER", operation = "修改部门")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DeptDTO dto) {
        deptService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除部门")
    @AuditLog(module = "USER", operation = "删除部门")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启停用部门")
    @AuditLog(module = "USER", operation = "启停用部门")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam @jakarta.validation.constraints.Min(0)
            @jakarta.validation.constraints.Max(1) Integer status) {
        deptService.changeStatus(id, status);
        return R.ok(null, "操作成功");
    }
}