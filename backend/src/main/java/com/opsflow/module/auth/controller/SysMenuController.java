package com.opsflow.module.auth.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.model.dto.MenuDTO;
import com.opsflow.module.auth.model.vo.MenuAdminVO;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.auth.service.MenuService;
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
 * 菜单管理接口（需 sys:menu:manage 权限）
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuService menuService;

    @Operation(summary = "查询菜单树")
    @PreAuthorize("hasAuthority('sys:menu:manage')")
    @GetMapping("/tree")
    public R<List<MenuAdminVO>> tree(@RequestParam(required = false) String menuName,
                                     @RequestParam(required = false) Integer status) {
        return R.ok(menuService.tree(menuName, status));
    }

    @Operation(summary = "新增菜单")
    @AuditLog(module = "USER", operation = "新增菜单")
    @PreAuthorize("hasAuthority('sys:menu:manage')")
    @PostMapping
    public R<Void> create(@Valid @RequestBody MenuDTO dto) {
        menuService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改菜单")
    @AuditLog(module = "USER", operation = "修改菜单")
    @PreAuthorize("hasAuthority('sys:menu:manage')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody MenuDTO dto) {
        menuService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除菜单")
    @AuditLog(module = "USER", operation = "删除菜单")
    @PreAuthorize("hasAuthority('sys:menu:manage')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启停用菜单")
    @AuditLog(module = "USER", operation = "启停用菜单")
    @PreAuthorize("hasAuthority('sys:menu:manage')")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam @jakarta.validation.constraints.Min(0)
            @jakarta.validation.constraints.Max(1) Integer status) {
        menuService.changeStatus(id, status);
        return R.ok(null, "操作成功");
    }
}