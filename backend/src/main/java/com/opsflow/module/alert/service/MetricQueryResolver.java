package com.opsflow.module.alert.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 语义指标 → PromQL 表达式解析器。
 * 将 alert_rule 中的语义指标（cpu_usage/memory_usage/disk_usage）映射为
 * 基于 node-exporter 的 PromQL 表达式，模板内以 {instance} 占位。
 */
@Component
public class MetricQueryResolver {

    private static final Map<String, String> TEMPLATES = Map.of(
            "cpu_usage", "100 - avg(rate(node_cpu_seconds_total{instance=\"{instance}\",mode=\"idle\"}[2m])) * 100",
            "memory_usage", "(1 - node_memory_MemAvailable_bytes{instance=\"{instance}\"} / node_memory_MemTotal_bytes{instance=\"{instance}\"}) * 100",
            "disk_usage", "(1 - node_filesystem_avail_bytes{instance=\"{instance}\",mountpoint=\"/\"} / node_filesystem_size_bytes{instance=\"{instance}\",mountpoint=\"/\"}) * 100"
    );

    /**
     * 将语义指标和实例标识生成为 PromQL 表达式。
     *
     * @param metric   语义指标名
     * @param instance 实例标识，如 192.168.2.136:9100
     * @return PromQL 表达式；无法映射的指标返回 null
     */
    public String promql(String metric, String instance) {
        String template = TEMPLATES.get(metric);
        if (template == null) {
            return null;
        }
        return template.replace("{instance}", instance);
    }
}