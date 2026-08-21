package com.opsflow.module.automation.rabbit;

import com.opsflow.common.util.AesUtil;
import com.opsflow.module.automation.mapper.AutoExecLogMapper;
import com.opsflow.module.automation.mapper.AutoExecRecordMapper;
import com.opsflow.module.automation.mapper.AutoScriptMapper;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.AutoExecLog;
import com.opsflow.module.automation.model.entity.AutoExecRecord;
import com.opsflow.module.automation.model.entity.AutoScript;
import com.opsflow.module.automation.model.entity.CmdbHost;
import com.opsflow.module.automation.ssh.SshExecutor;
import com.opsflow.module.automation.ws.ExecutionWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脚本执行消费者：接收执行任务 → SSH 远程执行 → 落库输出日志 + WebSocket 实时推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionConsumer {

    private static final int TIMEOUT_EXIT_CODE = -100;

    private final AutoExecRecordMapper recordMapper;
    private final AutoScriptMapper scriptMapper;
    private final CmdbHostMapper hostMapper;
    private final AutoExecLogMapper logMapper;
    private final SshExecutor sshExecutor;

    @RabbitListener(queues = AutomationRabbitConfig.EXEC_QUEUE)
    public void onMessage(Map<String, Long> message) {
        Long recordId = message == null ? null : message.get("recordId");
        if (recordId == null) {
            log.warn("收到非法执行消息: {}", message);
            return;
        }
        AutoExecRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            log.warn("执行记录不存在 recordId={}", recordId);
            return;
        }
        // 已被取消则跳过
        if (record.getStatus() != null && record.getStatus() == 6) {
            return;
        }

        AutoScript script = scriptMapper.selectById(record.getScriptId());
        CmdbHost host = hostMapper.selectById(record.getHostId());
        if (script == null || host == null) {
            fail(record, "脚本或主机不存在");
            return;
        }

        // 标记执行中
        record.setStatus(2);
        record.setStartTime(LocalDateTime.now());
        recordMapper.updateById(record);

        long startNano = System.nanoTime();
        AtomicInteger line = new AtomicInteger(0);
        try {
            String command = AesUtil.decrypt(script.getContent());
            String credential = AesUtil.decrypt(host.getCredential());
            int timeout = script.getTimeoutSeconds() == null ? 300 : script.getTimeoutSeconds();

            String sshUser = StringUtils.hasText(host.getSshUser()) ? host.getSshUser() : "root";
            int exitCode = sshExecutor.execute(host.getIpAddress(), host.getSshPort(), sshUser,
                    host.getAuthType(), credential, command, timeout, (streamType, content) -> {
                        // 落库 + WebSocket 实时推送
                        AutoExecLog logRow = new AutoExecLog();
                        logRow.setExecRecordId(recordId);
                        logRow.setStreamType(streamType);
                        logRow.setLineNumber(line.incrementAndGet());
                        logRow.setContent(content);
                        logRow.setTimestamp(LocalDateTime.now());
                        logMapper.insert(logRow);
                        ExecutionWebSocketHandler.sendToRecord(recordId,
                                "{\"streamType\":" + streamType + ",\"line\":" + logRow.getLineNumber()
                                        + ",\"content\":" + escapeJson(content) + "}");
                    });

            long durationMs = (System.nanoTime() - startNano) / 1_000_000;
            record.setExitCode(exitCode);
            record.setDurationMs(durationMs);
            record.setEndTime(LocalDateTime.now());
            if (exitCode == -100) {
                record.setStatus(5); // 超时
                record.setErrorMessage("脚本执行超时(>" + timeout + "s)");
            } else {
                record.setStatus(exitCode == 0 ? 3 : 4);
                if (exitCode != 0) {
                    record.setErrorMessage("脚本退出码非 0: " + exitCode);
                }
            }
            recordMapper.updateById(record);
            ExecutionWebSocketHandler.sendToRecord(recordId,
                    "{\"streamType\":0,\"line\":0,\"content\":\"[执行结束] exitCode=" + exitCode + "\"}");
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startNano) / 1_000_000;
            log.error("执行失败 recordId={}", recordId, e);
            record.setStatus(4);
            record.setExitCode(1);
            record.setDurationMs(durationMs);
            record.setEndTime(LocalDateTime.now());
            record.setErrorMessage(e.getMessage() != null ? e.getMessage() : "执行异常");
            recordMapper.updateById(record);
            ExecutionWebSocketHandler.sendToRecord(recordId,
                    "{\"streamType\":2,\"line\":0,\"content\":\"[执行失败] " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void fail(AutoExecRecord record, String message) {
        record.setStatus(4);
        record.setEndTime(LocalDateTime.now());
        record.setErrorMessage(message);
        recordMapper.updateById(record);
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "\\n") + "\"";
    }
}