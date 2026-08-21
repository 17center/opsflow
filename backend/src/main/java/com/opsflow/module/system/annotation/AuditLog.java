package com.opsflow.module.system.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志注解
 * 标注在 Controller 方法上，由 {@link com.opsflow.module.system.aspect.AuditLogAspect} 切面拦截记录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * 所属模块（USER/TICKET/SCRIPT/ALERT）
     */
    String module() default "USER";

    /**
     * 操作描述，如"新增用户"
     */
    String operation() default "";
}