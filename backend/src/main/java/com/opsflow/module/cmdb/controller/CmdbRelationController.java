package com.opsflow.module.cmdb.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.cmdb.model.dto.CmdbRelationDTO;
import com.opsflow.module.cmdb.model.vo.CmdbRelationVO;
import com.opsflow.module.cmdb.model.vo.CmdbTopologyVO;
import com.opsflow.module.cmdb.service.CmdbRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资产关联接口
 */
@Tag(name = "资产关联")
@RestController
@RequestMapping("/api/cmdb/relations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('cmdb:service:list')")
public class CmdbRelationController {

    private final CmdbRelationService cmdbRelationService;

    @Operation(summary = "关联关系列表")
    @GetMapping
    public R<List<CmdbRelationVO>> list() {
        return R.ok(cmdbRelationService.list());
    }

    @Operation(summary = "建立资产关联")
    @PostMapping
    public R<Void> create(@Valid @RequestBody CmdbRelationDTO dto) {
        cmdbRelationService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "关联成功");
    }

    @Operation(summary = "删除资产关联")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cmdbRelationService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "资产拓扑图")
    @GetMapping("/topology")
    public R<CmdbTopologyVO> topology() {
        return R.ok(cmdbRelationService.topology());
    }
}