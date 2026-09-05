package com.opsflow.module.automation.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册：/ws/exec?recordId={id}
 */
@Configuration
@EnableWebSocket
public class ExecutionWebSocketConfig implements WebSocketConfigurer {

    private final ExecutionWebSocketHandler executionWebSocketHandler;
    private final TerminalWebSocketHandler terminalWebSocketHandler;

    public ExecutionWebSocketConfig(ExecutionWebSocketHandler executionWebSocketHandler,
                                     TerminalWebSocketHandler terminalWebSocketHandler) {
        this.executionWebSocketHandler = executionWebSocketHandler;
        this.terminalWebSocketHandler = terminalWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(executionWebSocketHandler, "/ws/exec")
                .setAllowedOrigins("*");
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
                .setAllowedOrigins("*");
    }
}