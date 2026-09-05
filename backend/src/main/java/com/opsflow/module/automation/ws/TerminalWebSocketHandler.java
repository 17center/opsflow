package com.opsflow.module.automation.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.opsflow.common.util.AesUtil;
import com.opsflow.common.util.JwtUtil;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web 终端 WebSocket 处理器
 * 客户端连接: /ws/terminal?hostId={id}&token={jwt}
 * 建立后双向桥接 WebSocket ↔ SSH Shell
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final CmdbHostMapper hostMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** WebSocket session ID → SshContext */
    private final ConcurrentHashMap<String, SshContext> contexts = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        String query = uri.getQuery();
        if (query == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        String hostIdStr = null;
        String token = null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2) {
                if ("hostId".equals(kv[0])) hostIdStr = kv[1];
                if ("token".equals(kv[0])) token = kv[1];
            }
        }

        // 验证 token
        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("Web终端连接被拒绝：token 无效");
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 查主机
        if (hostIdStr == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        Long hostId;
        try {
            hostId = Long.parseLong(hostIdStr);
        } catch (NumberFormatException e) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        CmdbHost host = hostMapper.selectById(hostId);
        if (host == null || (host.getDeleted() != null && host.getDeleted() == 1)) {
            sendError(session, "主机不存在");
            close(session, CloseStatus.NORMAL);
            return;
        }

        // 解密凭据
        String credential;
        try {
            credential = AesUtil.decrypt(host.getCredential());
        } catch (Exception e) {
            log.error("Web终端：解密凭据失败 hostId={}", hostId, e);
            sendError(session, "凭据解密失败");
            close(session, CloseStatus.SERVER_ERROR);
            return;
        }

        // 建立 SSH 连接
        try {
            JSch jsch = new JSch();
            int authType = host.getAuthType() != null ? host.getAuthType() : 1;
            if (authType == 2) {
                jsch.addIdentity("terminal-key", credential.getBytes(StandardCharsets.UTF_8), null, null);
            }
            String sshUser = StringUtils.hasText(host.getSshUser()) ? host.getSshUser() : "root";
            int sshPort = host.getSshPort() != null ? host.getSshPort() : 22;
            Session sshSession = jsch.getSession(sshUser, host.getIpAddress(), sshPort);
            if (authType == 1) {
                sshSession.setPassword(credential == null ? "" : credential);
            }
            sshSession.setConfig("StrictHostKeyChecking", "no");
            sshSession.setConfig("PreferredAuthentications", authType == 1 ? "password,keyboard-interactive" : "publickey");
            sshSession.setServerAliveInterval(60_000);
            sshSession.connect(10_000);

            ChannelShell channel = (ChannelShell) sshSession.openChannel("shell");
            channel.setPtyType("xterm", 80, 24, 80 * 8, 24 * 16);
            channel.connect(10_000);

            SshContext ctx = new SshContext(sshSession, channel);
            contexts.put(session.getId(), ctx);

            // 启动输出桥接线程
            startOutputBridge(session, channel);

            log.info("Web终端连接成功 session={} host={}:{}", session.getId(), host.getIpAddress(), sshPort);
        } catch (Exception e) {
            log.error("Web终端 SSH 连接失败 hostId={}", hostId, e);
            sendError(session, "SSH 连接失败: " + e.getMessage());
            close(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        SshContext ctx = contexts.get(session.getId());
        if (ctx == null || ctx.channel == null || !ctx.channel.isConnected()) {
            return;
        }

        String payload = message.getPayload();

        // 检查是否为 resize 消息
        if (payload.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(payload);
                if ("resize".equals(node.path("type").asText())) {
                    int cols = node.path("cols").asInt(80);
                    int rows = node.path("rows").asInt(24);
                    ctx.channel.setPtySize(cols, rows, cols * 8, rows * 16);
                    return;
                }
            } catch (Exception ignored) {
                // 不是 JSON，当作普通输入处理
            }
        }

        // 普通输入写入 SSH channel
        try {
            OutputStream os = ctx.channel.getOutputStream();
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (Exception e) {
            log.debug("Web终端写入 SSH 失败 session={}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session.getId());
        log.info("Web终端连接关闭 session={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Web终端传输异常 session={}", session.getId(), exception);
        cleanup(session.getId());
    }

    /** SSH 输出 → WebSocket 输出 */
    private void startOutputBridge(WebSocketSession session, ChannelShell channel) {
        Thread t = new Thread(() -> {
            try (InputStream in = channel.getInputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while (channel.isConnected() && !Thread.currentThread().isInterrupted()) {
                    n = in.read(buf);
                    if (n > 0) {
                        String data = new String(buf, 0, n, StandardCharsets.UTF_8);
                        if (session.isOpen()) {
                            synchronized (session) {
                                session.sendMessage(new TextMessage(data));
                            }
                        }
                    } else if (n == -1) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("Web终端输出桥接结束 session={}", session.getId());
            }
            // SSH 通道关闭时关闭 WebSocket
            if (session.isOpen()) {
                close(session, CloseStatus.NORMAL);
            }
        }, "ssh-terminal-out-" + session.getId());
        t.setDaemon(true);
        t.start();
    }

    private void cleanup(String sessionId) {
        SshContext ctx = contexts.remove(sessionId);
        if (ctx != null) {
            try { if (ctx.channel != null) ctx.channel.disconnect(); } catch (Exception ignored) {}
            try { if (ctx.session != null) ctx.session.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void sendError(WebSocketSession session, String msg) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("\r\n*** " + msg + " ***\r\n"));
            } catch (Exception ignored) {}
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {}
    }

    private record SshContext(Session session, ChannelShell channel) {}
}
