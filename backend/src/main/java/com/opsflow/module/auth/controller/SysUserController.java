package com.opsflow.module.auth.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.model.dto.UserCreateDTO;
import com.opsflow.module.auth.model.dto.UserResetPwdDTO;
import com.opsflow.module.auth.model.dto.UserStatusDTO;
import com.opsflow.module.auth.model.dto.UserUpdateDTO;
import com.opsflow.module.auth.model.vo.UserVO;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.auth.service.UserService;
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

/**
 * 用户管理接口（需 sys:user:manage 权限）
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @GetMapping
    public R<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(userService.page(current, size, username, nickname, status, deptId));
    }

    @Operation(summary = "新增用户")
    @AuditLog(module = "USER", operation = "新增用户")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @PostMapping
    public R<Void> create(@Valid @RequestBody UserCreateDTO dto) {
        userService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改用户")
    @AuditLog(module = "USER", operation = "修改用户")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除用户")
    @AuditLog(module = "USER", operation = "删除用户")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id, SecurityUtils.getLoginUser().getUserId());
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "启停用用户")
    @AuditLog(module = "USER", operation = "启停用用户")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        userService.changeStatus(id, dto.getStatus(), SecurityUtils.getLoginUser().getUserId());
        return R.ok(null, "操作成功");
    }

    @Operation(summary = "重置密码")
    @AuditLog(module = "USER", operation = "重置密码")
    @PreAuthorize("hasAuthority('sys:user:manage')")
    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserResetPwdDTO dto) {
        userService.resetPassword(id, dto.getPassword(), SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "重置成功");
    }
}
