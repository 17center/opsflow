package com.opsflow.module.system.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.model.vo.NotificationVO;
import com.opsflow.module.system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站内通知接口
 */
@Tag(name = "站内通知")
@RestController
@RequestMapping("/api/system/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "分页查询我的通知")
    @GetMapping
    public R<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer isRead) {
        if (size > 100) {
            size = 100;
        }
        LoginUser user = SecurityUtils.getLoginUser();
        return R.ok(notificationService.page(user.getUserId(), current, size, isRead));
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public R<Map<String, Long>> unreadCount() {
        LoginUser user = SecurityUtils.getLoginUser();
        return R.ok(Map.of("count", notificationService.unreadCount(user.getUserId())));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        notificationService.markRead(id, user.getUserId());
        return R.ok(null, "操作成功");
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        LoginUser user = SecurityUtils.getLoginUser();
        notificationService.markAllRead(user.getUserId());
        return R.ok(null, "操作成功");
    }
}