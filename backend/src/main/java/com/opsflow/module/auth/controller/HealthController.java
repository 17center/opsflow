package com.opsflow.module.auth.controller;

import com.opsflow.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（骨架验证用）
 */
@Tag(name = "系统健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "opsflow-backend");
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return R.ok(data);
    }
}