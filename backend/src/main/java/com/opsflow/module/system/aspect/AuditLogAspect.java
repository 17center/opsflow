package com.opsflow.module.system.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.module.auth.security.LoginUser;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.system.annotation.AuditLog;
import com.opsflow.module.system.mapper.SysAuditLogMapper;
import com.opsflow.module.system.model.entity.SysAuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作审计日志切面
 * 拦截标注了 @AuditLog 的方法，记录操作人、请求参数、响应结果、耗时、IP 等。
 * 注：审计记录写入失败不影响主流程，异常吞掉仅记录日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    /** 响应结果 / 参数最大存储长度 */
    private static final int MAX_LENGTH = 2000;
    /** 需脱敏的关键字 */
    private static final String[] SENSITIVE_KEYWORDS = {"password", "passwd", "token", "secret", "authorization", "oldPassword", "newPassword"};

    private final SysAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysAuditLog record = new SysAuditLog();
        record.setCreateTime(LocalDateTime.now());
        fillLoginInfo(record);
        fillRequestInfo(record, joinPoint, auditLog);

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            record.setStatus(1);
            return result;
        } catch (Throwable e) {
            error = e;
            record.setStatus(0);
            record.setErrorMessage(truncate(e.getMessage()));
            throw e;
        } finally {
            record.setDurationMs(System.currentTimeMillis() - start);
            if (record.getStatus() != null && record.getStatus() == 1) {
                record.setResponseResult(truncate(serialize(result)));
            }
            try {
                auditLogMapper.insert(record);
            } catch (Exception e) {
                log.warn("写入审计日志失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 填充当前登录用户信息
     */
    private void fillLoginInfo(SysAuditLog record) {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            record.setUserId(loginUser.getUserId());
            record.setUsername(loginUser.getUsername());
        } catch (Exception e) {
            // 未登录或系统自动操作，保持为空
        }
    }

    /**
     * 填充请求相关信息和注解元数据
     */
    private void fillRequestInfo(SysAuditLog record, ProceedingJoinPoint joinPoint, AuditLog auditLog) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            record.setRequestUrl(request.getRequestURI());
            record.setRequestMethod(request.getMethod());
            record.setUserAgent(truncate(request.getHeader("User-Agent")));
            record.setIp(getClientIp(request));
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        record.setModule(auditLog.module());
        record.setOperation(auditLog.operation());
        record.setMethod(signature.getDeclaringType().getSimpleName() + "." + signature.getName());
        record.setRequestParams(truncate(serialize(joinPoint.getArgs())));
    }

    /**
     * 获取客户端 IP（兼容反向代理 X-Forwarded-For）
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 序列化请求参数 / 响应结果，脱敏并限制长度
     */
    private String serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof Object[] args) {
                List<Object> filtered = new ArrayList<>();
                for (Object arg : args) {
                    if (arg instanceof MultipartFile) {
                        filtered.add("[file]");
                    } else {
                        filtered.add(arg);
                    }
                }
                return maskSensitive(objectMapper.writeValueAsString(filtered));
            }
            return maskSensitive(objectMapper.writeValueAsString(obj));
        } catch (Exception e) {
            return maskSensitive(String.valueOf(obj));
        }
    }

    /**
     * 对 JSON 字符串中的敏感字段值做脱敏（如 password、token 等）
     * 例如 {"password":"admin123"} -> {"password":"***"}
     */
    private String maskSensitive(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        for (String keyword : SENSITIVE_KEYWORDS) {
            json = json.replaceAll("(?i)(\"" + keyword + "\"\\s*:\\s*\")[^\"]*(\")", "$1***$2");
        }
        return json;
    }

    /**
     * 截断超长内容
     */
    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
    }
}