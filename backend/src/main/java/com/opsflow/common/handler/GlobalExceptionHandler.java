package com.opsflow.common.handler;

import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常 (@Validated)
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.PARAM_ERROR.getMessage()
                : fieldError.getDefaultMessage();
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 权限不足
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage());
    }

    /**
     * 兜底异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ErrorCode.SERVER_ERROR.getCode(), ErrorCode.SERVER_ERROR.getMessage());
    }
}