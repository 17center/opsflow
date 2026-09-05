package com.opsflow.module.alert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Prometheus HTTP API 客户端
 * 使用 JDK 内置 HttpClient 调用 instant query：GET /api/v1/query?query={PromQL}
 */
@Slf4j
@Component
public class PrometheusClient {

    private final String prometheusUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public PrometheusClient(@Value("${opsflow.monitoring.prometheus-url:http://localhost:9090}") String prometheusUrl) {
        this.prometheusUrl = prometheusUrl.endsWith("/")
                ? prometheusUrl.substring(0, prometheusUrl.length() - 1)
                : prometheusUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * 执行 PromQL 即时查询，返回第一个时间序列的当前值（Double）。
     *
     * @param promql PromQL 表达式
     * @return 指标值；无数据/请求失败/统计出错返回 null
     */
    public Double queryValue(String promql) {
        try {
            URI uri = URI.create(prometheusUrl + "/api/v1/query?query=" + java.net.URLEncoder.encode(promql, java.nio.charset.StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Prometheus 查询失败 status={} query={}", response.statusCode(), promql);
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            if (!"success".equals(status)) {
                log.warn("Prometheus 查询 status={} error={}", status, root.path("error").asText());
                return null;
            }
            JsonNode results = root.path("data").path("result");
            if (results.isEmpty()) {
                log.debug("Prometheus 查询无数据 query={}", promql);
                return null;
            }
            JsonNode value = results.get(0).path("value");
            if (value.size() < 2) {
                return null;
            }
            String number = value.get(1).asText();
            if (number == null || "NaN".equalsIgnoreCase(number) || "+Inf".equalsIgnoreCase(number) || "-Inf".equalsIgnoreCase(number)) {
                return null;
            }
            return Double.parseDouble(number);
        } catch (Exception e) {
            log.warn("Prometheus 即时查询异常 query={}: {}", promql, e.getMessage());
            return null;
        }
    }
}