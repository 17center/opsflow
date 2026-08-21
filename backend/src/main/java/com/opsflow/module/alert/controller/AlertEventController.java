package com.opsflow.module.alert.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.alert.model.dto.AlertSilenceDTO;
import com.opsflow.module.alert.model.vo.AlertEventVO;
import com.opsflow.module.alert.model.vo.AlertStatsVO;
import com.opsflow.module.alert.service.AlertEventService;
import com.opsflow.module.auth.security.SecurityUtils;
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

/**
 * 告警事件接口
 */
@Tag(name = "告警事件")
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('alert:event:list')")
public class AlertEventController {

    private final AlertEventService eventService;

    @Operation(summary = "分页查询告警事件")
    @GetMapping("/events")
    public R<PageResult<AlertEventVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer alertLevel,
            @RequestParam(required = false) Long hostId,
            @RequestParam(required = false) String keyword) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(eventService.page(current, size, status, alertLevel, hostId, keyword));
    }

    @Operation(summary = "告警事件详情")
    @GetMapping("/events/{id}")
    public R<AlertEventVO> detail(@PathVariable Long id) {
        return R.ok(eventService.detail(id));
    }

    @Operation(summary = "确认告警")
    @PostMapping("/events/{id}/confirm")
    public R<Void> confirm(@PathVariable Long id) {
        eventService.confirm(id, SecurityUtils.getLoginUser().getUserId());
        return R.ok(null, "已确认");
    }

    @Operation(summary = "静默告警")
    @PostMapping("/events/{id}/silence")
    public R<Void> silence(@PathVariable Long id, @Valid @RequestBody AlertSilenceDTO dto) {
        eventService.silence(id, dto.getSilenceMinutes());
        return R.ok(null, "已静默");
    }

    @Operation(summary = "恢复告警")
    @PostMapping("/events/{id}/recover")
    public R<Void> recover(@PathVariable Long id) {
        eventService.recover(id);
        return R.ok(null, "已恢复");
    }

    @Operation(summary = "告警统计")
    @GetMapping("/stats")
    public R<AlertStatsVO> stats() {
        return R.ok(eventService.stats());
    }

    @Operation(summary = "模拟触发告警（供指标接入/测试）")
    @PostMapping("/events/trigger")
    public R<AlertEventVO> trigger(
            @RequestParam Long ruleId,
            @RequestParam(required = false) Long hostId,
            @RequestParam java.math.BigDecimal currentValue) {
        return R.ok(eventService.toVOForController(
                eventService.trigger(ruleId, hostId, currentValue)));
    }
}