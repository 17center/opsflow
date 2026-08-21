package com.opsflow.module.alert.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.alert.model.dto.AlertDutyDTO;
import com.opsflow.module.alert.model.vo.AlertDutyVO;
import com.opsflow.module.alert.service.AlertDutyService;
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

import java.util.List;

/**
 * 值班排班接口
 */
@Tag(name = "值班排班")
@RestController
@RequestMapping("/api/alerts/duty")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('alert:duty:list')")
public class AlertDutyController {

    private final AlertDutyService dutyService;

    @Operation(summary = "按月查询排班")
    @GetMapping
    public R<List<AlertDutyVO>> listByMonth(@RequestParam String month) {
        return R.ok(dutyService.listByMonth(month));
    }

    @Operation(summary = "创建排班")
    @PostMapping
    public R<Void> create(@Valid @RequestBody AlertDutyDTO dto) {
        dutyService.create(dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改排班")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AlertDutyDTO dto) {
        dutyService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除排班")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dutyService.delete(id);
        return R.ok(null, "删除成功");
    }
}