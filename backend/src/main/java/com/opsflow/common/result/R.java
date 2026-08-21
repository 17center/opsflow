package com.opsflow.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功，其他为错误码 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public R() {
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(T data, String message) {
        return new R<>(200, message, data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }
}