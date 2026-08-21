package com.opsflow.module.automation.ssh;

/**
 * SSH 执行异常
 */
public class SshExecutionException extends RuntimeException {

    public SshExecutionException(String message) {
        super(message);
    }

    public SshExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}