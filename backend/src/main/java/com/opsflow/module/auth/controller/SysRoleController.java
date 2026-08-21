package com.opsflow.module.auth.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.model.dto.RoleAssignDTO;
import com.opsflow.module.auth.model.dto.RoleCreateDTO;
import com.opsflow.module.auth.model.dto.RoleStatusDTO;
import com.opsflow.module.auth.model.dto.RoleUpdateDTO;
import com.opsflow.module.auth.model.vo.MenuTreeVO;
import com.opsflow.module.auth.model.vo.RoleAdminVO;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.auth.service.RoleService;
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
 * 角色管理接口（需 sys:role:manage 权限）
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final RoleService roleService;

    @Operation(summary = "分页查询角色")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @GetMapping
    public R<PageResult<RoleAdminVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(roleService.page(current, size, roleName, roleCode, status));
    }

    @Operation(summary = "角色详情（含已分配菜单）")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @GetMapping("/{id}")
    public R<RoleAdminVO> detail(@PathVariable Long id) {
        return R.ok(roleService.detail(id));
    }

    @Operation(summary = "新增角色")
    @AuditLog(module = "USER", operation = "新增角色")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @PostMapping
    public R<Void> create(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改角色")
    @AuditLog(module = "USER", operation = "修改角色")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除角色")
    @AuditLog(module = "USER", operation = "删除角色")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启停用角色")
    @AuditLog(module = "USER", operation = "启停用角色")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody RoleStatusDTO dto) {
        roleService.changeStatus(id, dto.getStatus());
        return R.ok(null, "操作成功");
    }

    @Operation(summary = "分配权限（角色-菜单）")
    @AuditLog(module = "USER", operation = "分配权限")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @PutMapping("/{id}/menus")
    public R<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleAssignDTO dto) {
        roleService.assignMenus(id, dto.getMenuIds());
        return R.ok(null, "分配成功");
    }

    @Operation(summary = "查询全部菜单树（分配权限用）")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    @GetMapping("/menus/tree")
    public R<List<MenuTreeVO>> menuTree() {
        return R.ok(roleService.menuTree());
    }
}