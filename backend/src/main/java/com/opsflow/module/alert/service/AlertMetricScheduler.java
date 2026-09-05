package com.opsflow.module.alert.service;

import com.opsflow.module.alert.model.entity.AlertRule;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 告警指标定时采集器。
 * 定时从 Prometheus 拉取已启用告警规则对应的真实指标值，
 * 满足阈值则触发告警，不满足则恢复未关闭事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMetricScheduler {

    private final AlertRuleService ruleService;
    private final AlertEventService eventService;
    private final PrometheusClient prometheusClient;
    private final MetricQueryResolver metricQueryResolver;
    private final CmdbHostMapper hostMapper;

    @Value("${opsflow.monitoring.enabled:true}")
    private boolean enabled;

    @Value("${opsflow.monitoring.node-exporter-port:9100}")
    private int nodeExporterPort;

    @Scheduled(fixedDelayString = "${opsflow.monitoring.scrape-interval-ms:15000}")
    public void scrape() {
        if (!enabled) {
            return;
        }
        List<AlertRule> rules;
        try {
            rules = ruleService.listEnabled();
        } catch (Exception e) {
            log.error("获取启用告警规则失败", e);
            return;
        }
        if (rules.isEmpty()) {
            return;
        }

        for (AlertRule rule : rules) {
            if (rule.getHostId() == null) {
                continue; // 全局规则无目标主机，跳过
            }
            try {
                scrapeRule(rule);
            } catch (Exception e) {
                log.error("采集告警规则异常 ruleId={}", rule.getId(), e);
            }
        }
    }

    private void scrapeRule(AlertRule rule) {
        CmdbHost host = hostMapper.selectById(rule.getHostId());
        if (host == null || (host.getDeleted() != null && host.getDeleted() == 1)) {
            return;
        }

        String instance = host.getIpAddress() + ":" + nodeExporterPort;
        String promql = metricQueryResolver.promql(rule.getMetric(), instance);
        if (promql == null) {
            return; // 未映射的指标，跳过（向后兼容）
        }

        Double value = prometheusClient.queryValue(promql);
        if (value == null) {
            // 无数据：无法判定，若已有未关闭事件则恢复以避免误报阻塞
            eventService.recoverByRule(rule.getId(), host.getId());
            return;
        }

        log.info("采集指标 ruleId={} metric={} instance={} value={} threshold={}{}",
                rule.getId(), rule.getMetric(), instance, value,
                rule.getOperator(), rule.getThreshold());

        if (match(rule.getOperator(), value, rule.getThreshold())) {
            eventService.trigger(rule.getId(), host.getId(), BigDecimal.valueOf(value));
        } else {
            eventService.recoverByRule(rule.getId(), host.getId());
        }
    }

    private boolean match(String operator, double value, BigDecimal threshold) {
        double t = threshold == null ? 0 : threshold.doubleValue();
        return switch (operator == null ? ">" : operator) {
            case ">" -> value > t;
            case ">=" -> value >= t;
            case "<" -> value < t;
            case "<=" -> value <= t;
            case "==" -> value == t;
            default -> false;
        };
    }
}