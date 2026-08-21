package com.opsflow.module.automation.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 基于 JSch 的 SSH 远程执行器
 * 支持密码与私钥认证，流式输出 stdout/stderr，支持超时控制。
 */
@Slf4j
@Component
public class SshExecutor {

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /**
     * 执行远程命令
     *
     * @param host         主机 IP
     * @param port         SSH 端口
     * @param username     用户名
     * @param authType     1=密码 2=私钥
     * @param credential   密码明文 或 私钥内容
     * @param command      待执行命令
     * @param timeoutS     超时秒数
     * @param lineConsumer 输出行回调 (streamType: 1=stdout 2=stderr, line)
     * @return 退出码
     */
    public int execute(String host, int port, String username, int authType, String credential,
                       String command, int timeoutS, BiConsumer<Integer, String> lineConsumer) {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            if (authType == 2) {
                jsch.addIdentity("exec-key", credential.getBytes(StandardCharsets.UTF_8), null, null);
            }
            session = jsch.getSession(username, host, port);
            if (authType == 1) {
                session.setPassword(credential == null ? "" : credential);
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", authType == 1 ? "password,keyboard-interactive" : "publickey");
            session.connect(CONNECT_TIMEOUT_MS);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setPty(false);
            channel.setInputStream(null);

            InputStream out = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect(CONNECT_TIMEOUT_MS);

            AtomicBoolean done = new AtomicBoolean(false);
            AtomicInteger exitCode = new AtomicInteger(-1);
            CountDownLatch streamLatch = new CountDownLatch(2);

            Thread t1 = streamThread(out, 1, lineConsumer, done, streamLatch);
            Thread t2 = streamThread(err, 2, lineConsumer, done, streamLatch);

            // 超时等待退出码
            long deadline = System.currentTimeMillis() + timeoutS * 1000L;
            while (System.currentTimeMillis() < deadline) {
                if (channel.isClosed()) {
                    exitCode.set(channel.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (!channel.isClosed()) {
                // 超时
                done.set(true);
                channel.disconnect();
                log.warn("SSH 执行超时 host={} cmd={}", host, command);
                return -100; // 约定超时退出码
            }
            done.set(true);
            streamLatch.await(2, TimeUnit.SECONDS);
            return exitCode.get();
        } catch (JSchException e) {
            log.error("SSH 连接/执行失败 host={}：{}", host, e.getMessage());
            throw new SshExecutionException("SSH 连接或执行失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("SSH 执行异常 host={}", host, e);
            throw new SshExecutionException("SSH 执行异常: " + e.getMessage(), e);
        } finally {
            if (channel != null) {
                try {
                    channel.disconnect();
                } catch (Exception ignored) {
                }
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private Thread streamThread(InputStream in, int streamType, BiConsumer<Integer, String> consumer,
                                AtomicBoolean done, CountDownLatch latch) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (consumer != null && StringUtils.hasText(line)) {
                        consumer.accept(streamType, line);
                    }
                }
            } catch (Exception e) {
                log.debug("读取输出流结束/异常 stream={}", streamType, e);
            } finally {
                latch.countDown();
            }
        }, "ssh-stream-" + streamType);
        t.setDaemon(true);
        t.start();
        return t;
    }
}