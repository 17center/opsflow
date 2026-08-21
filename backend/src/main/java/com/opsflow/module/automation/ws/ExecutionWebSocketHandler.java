package com.opsflow.module.automation.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行输出 WebSocket 处理器
 * 客户端连接: /ws/exec?recordId={execRecordId}
 * 执行引擎通过 sendToRecord 将实时输出推送给订阅该执行记录的所有会话
 */
@Component
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    /** recordId -> 会话集合 */
    private static final Map<Long, Map<String, WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long recordId = parseRecordId(session);
        if (recordId == null) {
            return;
        }
        SESSIONS
                .computeIfAbsent(recordId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long recordId = parseRecordId(session);
        if (recordId == null) {
            return;
        }
        Map<String, WebSocketSession> map = SESSIONS.get(recordId);
        if (map != null) {
            map.remove(session.getId());
            if (map.isEmpty()) {
                SESSIONS.remove(recordId);
            }
        }
    }

    /** 向指定执行记录的订阅会话推送一行输出 */
    public static void sendToRecord(Long recordId, String message) {
        Map<String, WebSocketSession> map = SESSIONS.get(recordId);
        if (map == null || map.isEmpty()) {
            return;
        }
        TextMessage text = new TextMessage(message);
        for (WebSocketSession session : map.values()) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(text);
                    }
                } catch (IOException e) {
                    // 单个会话推送失败不影响其他会话
                }
            }
        }
    }

    private Long parseRecordId(WebSocketSession session) {
        String query = session.getUri() == null ? null : session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        String[] params = query.split("&");
        for (String p : params) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2 && "recordId".equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}