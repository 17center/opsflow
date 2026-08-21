package com.opsflow.module.alert.service;

import com.opsflow.module.alert.mapper.AlertNotifyLogMapper;
import com.opsflow.module.alert.model.entity.AlertEvent;
import com.opsflow.module.alert.model.entity.AlertNotifyLog;
import com.opsflow.module.alert.model.entity.AlertRule;
import com.opsflow.module.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 告警通知服务：触发告警时向通知人/值班人生成站内消息，并记录通知日志
 * 实际邮件/飞书/钉钉 Webhook 推送由外部渠道接入，此处记录日志并占位
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotifyService {

    private final AlertNotifyLogMapper notifyLogMapper;
    private final NotificationService notificationService;

    /** 通知类型：3=告警 */
    private static final int NOTIFY_TYPE_ALERT = 3;

    /**
     * 触发告警通知
     *
     * @param rule   告警规则
     * @param event  告警事件
     * @param userIds 通知的用户 ID 列表
     */
    public void notifyAlert(AlertRule rule, AlertEvent event, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            log.info("告警事件 {} 未配置通知人，跳过通知", event.getId());
            return;
        }
        String title = "【" + levelName(rule.getAlertLevel()) + "告警】" + rule.getName();
        String content = "规则[" + rule.getName() + "]触发告警，指标=" + rule.getMetric()
                + "，当前值=" + event.getCurrentValue() + "，阈值=" + rule.getThreshold()
                + "，触发时间=" + event.getCreateTime();

        // 站内通知
        for (Long userId : userIds) {
            notificationService.notify(userId, title, content, NOTIFY_TYPE_ALERT, event.getId(), "ALERT");
        }

        // 针对已配置的渠道记录通知日志（占位实现，实际推送由外部渠道接入）
        List<String> channels = parseChannels(rule.getNotifyChannels());
        for (String channel : channels) {
            for (Long userId : userIds) {
                AlertNotifyLog logEntry = new AlertNotifyLog();
                logEntry.setEventId(event.getId());
                logEntry.setChannel(channel);
                logEntry.setReceiver(String.valueOf(userId));
                logEntry.setStatus(1);
                notifyLogMapper.insert(logEntry);
            }
        }
    }

    /** 解析逗号分隔的通知渠道 */
    private static List<String> parseChannels(String channels) {
        if (!StringUtils.hasText(channels)) {
            return List.of();
        }
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private static String levelName(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 0 -> "紧急";
            case 1 -> "高";
            case 2 -> "中";
            default -> "低";
        };
    }
}