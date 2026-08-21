package com.opsflow.module.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.automation.mapper.AutoExecLogMapper;
import com.opsflow.module.automation.mapper.AutoExecRecordMapper;
import com.opsflow.module.automation.mapper.AutoScriptMapper;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.dto.AutoExecStartDTO;
import com.opsflow.module.automation.model.entity.AutoExecLog;
import com.opsflow.module.automation.model.entity.AutoExecRecord;
import com.opsflow.module.automation.model.entity.AutoScript;
import com.opsflow.module.automation.model.entity.CmdbHost;
import com.opsflow.module.automation.model.vo.AutoExecDetailVO;
import com.opsflow.module.automation.model.vo.AutoExecLogVO;
import com.opsflow.module.automation.model.vo.AutoExecRecordVO;
import com.opsflow.module.automation.rabbit.AutomationRabbitConfig;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自动化执行服务：触发执行（投递 RabbitMQ）、查询记录/日志、取消任务
 */
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private static final String[] TRIGGER_NAMES = {"", "工单自动触发", "手动触发"};
    private static final String[] STATUS_NAMES = {"", "等待", "执行中", "成功", "失败", "超时", "取消"};

    private final AutoExecRecordMapper recordMapper;
    private final AutoExecLogMapper logMapper;
    private final AutoScriptMapper scriptMapper;
    private final CmdbHostMapper hostMapper;
    private final SysUserMapper userMapper;
    private final TicketMapper ticketMapper;
    private final RabbitTemplate rabbitTemplate;

    /** 触发执行：创建记录并投递消息到 RabbitMQ，由消费者异步执行 */
    @Transactional
    public Long start(AutoExecStartDTO dto, Long operatorId, String operatorName) {
        AutoScript script = scriptMapper.selectById(dto.getScriptId());
        if (script == null) {
            throw new BusinessException(ErrorCode.SCRIPT_NOT_FOUND);
        }
        if (script.getStatus() == null || script.getStatus() != 1) {
            throw new BusinessException(ErrorCode.SCRIPT_DISABLED);
        }
        CmdbHost host = hostMapper.selectById(dto.getHostId());
        if (host == null) {
            throw new BusinessException(ErrorCode.HOST_NOT_FOUND);
        }

        AutoExecRecord record = new AutoExecRecord();
        record.setTicketId(dto.getTicketId());
        record.setScriptId(script.getId());
        record.setScriptVersion(script.getCurrentVersion());
        record.setHostId(host.getId());
        record.setTriggerType(dto.getTriggerType() == null ? 2 : dto.getTriggerType());
        record.setStatus(1); // 等待
        record.setOperatorId(operatorId);
        record.setCreateBy(operatorName);
        recordMapper.insert(record);

        // 事务提交后再投递异步执行任务，避免消费者在事务未提交时读到不存在的记录
        final Long recordId = record.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(AutomationRabbitConfig.EXEC_QUEUE, Map.of("recordId", recordId));
            }
        });
        return recordId;
    }

    public PageResult<AutoExecRecordVO> page(long current, long size, Long scriptId, Long hostId, Integer status) {
        Page<AutoExecRecord> page = recordMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<AutoExecRecord>lambdaQuery()
                        .eq(scriptId != null, AutoExecRecord::getScriptId, scriptId)
                        .eq(hostId != null, AutoExecRecord::getHostId, hostId)
                        .eq(status != null, AutoExecRecord::getStatus, status)
                        .orderByDesc(AutoExecRecord::getCreateTime));
        return PageResult.of(toRecordVOList(page.getRecords()), page.getTotal(), page.getSize(), page.getCurrent());
    }

    public AutoExecDetailVO detail(Long recordId) {
        AutoExecRecord record = requireRecord(recordId);
        List<AutoExecLogVO> logs = logMapper.selectList(
                        Wrappers.<AutoExecLog>lambdaQuery()
                                .eq(AutoExecLog::getExecRecordId, recordId)
                                .orderByAsc(AutoExecLog::getLineNumber))
                .stream().map(this::toLogVO).collect(Collectors.toList());
        AutoExecDetailVO vo = new AutoExecDetailVO();
        vo.setRecord(buildRecordVO(record));
        vo.setLogs(logs);
        return vo;
    }

    /** 单个记录详情：补齐脚本名/主机/操作人信息 */
    private AutoExecRecordVO buildRecordVO(AutoExecRecord record) {
        AutoScript script = scriptMapper.selectById(record.getScriptId());
        CmdbHost host = hostMapper.selectById(record.getHostId());
        SysUser op = record.getOperatorId() == null ? null : userMapper.selectById(record.getOperatorId());
        Ticket ticket = record.getTicketId() == null ? null : ticketMapper.selectById(record.getTicketId());
        return toRecordVO(record, script, host, op, ticket);
    }

    /** 取消：仅等待/执行中可取消 */
    @Transactional
    public void cancel(Long recordId, String operator) {
        AutoExecRecord record = requireRecord(recordId);
        if (record.getStatus() != null && record.getStatus() >= 3) {
            throw new BusinessException(ErrorCode.EXEC_COMPLETED);
        }
        record.setStatus(6);
        record.setEndTime(java.time.LocalDateTime.now());
        record.setErrorMessage("任务已被取消");
        record.setUpdateBy(operator);
        recordMapper.updateById(record);
    }

    private AutoExecRecord requireRecord(Long id) {
        AutoExecRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.EXEC_NOT_FOUND);
        }
        return record;
    }

    private List<AutoExecRecordVO> toRecordVOList(List<AutoExecRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return List.of();
        }
        List<Long> scriptIds = records.stream().map(AutoExecRecord::getScriptId).distinct().collect(Collectors.toList());
        List<Long> hostIds = records.stream().map(AutoExecRecord::getHostId).distinct().collect(Collectors.toList());
        List<Long> operatorIds = records.stream().map(AutoExecRecord::getOperatorId).distinct().collect(Collectors.toList());
        List<Long> ticketIds = records.stream().map(AutoExecRecord::getTicketId).filter(id -> id != null).distinct().collect(Collectors.toList());

        Map<Long, AutoScript> scripts = scriptMapper.selectBatchIds(scriptIds).stream()
                .collect(Collectors.toMap(AutoScript::getId, Function.identity()));
        Map<Long, CmdbHost> hosts = hostMapper.selectBatchIds(hostIds).stream()
                .collect(Collectors.toMap(CmdbHost::getId, Function.identity()));
        Map<Long, SysUser> operators = userMapper.selectBatchIds(operatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, Ticket> tickets = ticketIds.isEmpty() ? Map.of()
                : ticketMapper.selectBatchIds(ticketIds).stream().collect(Collectors.toMap(Ticket::getId, Function.identity()));

        return records.stream().map(r -> toRecordVO(r,
                scripts.get(r.getScriptId()), hosts.get(r.getHostId()),
                operators.get(r.getOperatorId()), r.getTicketId() == null ? null : tickets.get(r.getTicketId())))
                .collect(Collectors.toList());
    }

    private AutoExecRecordVO toRecordVO(AutoExecRecord r, AutoScript s, CmdbHost h, SysUser op, Ticket t) {
        AutoExecRecordVO vo = new AutoExecRecordVO();
        vo.setId(r.getId());
        vo.setScriptId(r.getScriptId());
        vo.setScriptName(s == null ? null : s.getName());
        vo.setScriptVersion(r.getScriptVersion());
        vo.setHostId(r.getHostId());
        vo.setHostIp(h == null ? null : h.getIpAddress());
        vo.setHostname(h == null ? null : h.getHostname());
        vo.setTicketId(r.getTicketId());
        vo.setTicketNo(t == null ? null : t.getTicketNo());
        vo.setTriggerType(r.getTriggerType());
        vo.setTriggerTypeName(r.getTriggerType() != null && r.getTriggerType() < TRIGGER_NAMES.length ? TRIGGER_NAMES[r.getTriggerType()] : null);
        vo.setStatus(r.getStatus());
        vo.setStatusName(r.getStatus() != null && r.getStatus() < STATUS_NAMES.length ? STATUS_NAMES[r.getStatus()] : null);
        vo.setExitCode(r.getExitCode());
        vo.setStartTime(r.getStartTime());
        vo.setEndTime(r.getEndTime());
        vo.setDurationMs(r.getDurationMs());
        vo.setOperatorName(op == null ? null : (StringUtils.hasText(op.getNickname()) ? op.getNickname() : op.getUsername()));
        vo.setErrorMessage(r.getErrorMessage());
        vo.setCreateTime(r.getCreateTime());
        return vo;
    }

    private AutoExecLogVO toLogVO(AutoExecLog log) {
        AutoExecLogVO vo = new AutoExecLogVO();
        vo.setId(log.getId());
        vo.setStreamType(log.getStreamType());
        vo.setStreamTypeName(log.getStreamType() != null && log.getStreamType() == 2 ? "stderr" : "stdout");
        vo.setLineNumber(log.getLineNumber());
        vo.setContent(log.getContent());
        vo.setTimestamp(log.getTimestamp());
        return vo;
    }
}