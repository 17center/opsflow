package com.opsflow.module.cmdb.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.cmdb.model.dto.CmdbServiceDTO;
import com.opsflow.module.cmdb.model.vo.CmdbServiceVO;
import com.opsflow.module.cmdb.service.CmdbServiceService;
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
import java.util.Map;

/**
 * 服务资产接口
 */
@Tag(name = "服务资产")
@RestController
@RequestMapping("/api/cmdb/services")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('cmdb:service:list')")
public class CmdbServiceController {

    private final CmdbServiceService cmdbServiceService;

    @Operation(summary = "分页查询服务资产")
    @GetMapping
    public R<PageResult<CmdbServiceVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Integer status) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(cmdbServiceService.page(current, size, keyword, serviceType, status));
    }

    @Operation(summary = "服务资产详情")
    @GetMapping("/{id}")
    public R<CmdbServiceVO> detail(@PathVariable Long id) {
        return R.ok(cmdbServiceService.detail(id));
    }

    @Operation(summary = "新增服务资产")
    @PostMapping
    public R<Void> create(@Valid @RequestBody CmdbServiceDTO dto) {
        cmdbServiceService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "新增成功");
    }

    @Operation(summary = "修改服务资产")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CmdbServiceDTO dto) {
        cmdbServiceService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除服务资产")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cmdbServiceService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "自动发现主机服务")
    @PostMapping("/discover/{hostId}")
    public R<List<CmdbServiceService.DiscoveredService>> discover(@PathVariable Long hostId) {
        return R.ok(cmdbServiceService.autoDiscover(hostId));
    }

    @Operation(summary = "批量确认录入自动发现的服务")
    @PostMapping("/batch")
    public R<Void> batchCreate(@Valid @RequestBody List<CmdbServiceDTO> dtos) {
        String operator = SecurityUtils.getLoginUser().getUsername();
        for (CmdbServiceDTO dto : dtos) {
            cmdbServiceService.create(dto, operator);
        }
        return R.ok(null, "批量录入成功");
    }

    @Operation(summary = "状态变更（生命周期）")
    @PostMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        cmdbServiceService.changeStatus(id, body.get("status"), SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "状态已更新");
    }
}