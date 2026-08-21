package com.opsflow.module.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.alert.mapper.AlertEventMapper;
import com.opsflow.module.alert.model.entity.AlertEvent;
import com.opsflow.module.alert.model.entity.AlertRule;
import com.opsflow.module.alert.model.vo.AlertEventVO;
import com.opsflow.module.alert.model.vo.AlertStatsVO;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警事件服务：列表、确认、静默、恢复、统计、触发（含收敛）
 */
@Service
@RequiredArgsConstructor
public class AlertEventService {

    /** 状态：1=告警中 2=已确认 3=已恢复 4=已静默 */
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_CONFIRMED = 2;
    private static final int STATUS_RECOVERED = 3;
    private static final int STATUS_SILENCED = 4;

    private static final String[] STATUS_NAMES = {"", "告警中", "已确认", "已恢复", "已静默"};
    private static final String[] LEVEL_NAMES = {"紧急", "高", "中", "低"};

    /** 收敛窗口（分钟），相同规则相同主机 5 分钟内重复触发只生成一条 */
    private static final int CONVERGENCE_MINUTES = 5;

    private final AlertEventMapper eventMapper;
    private final AlertRuleService ruleService;
    private final AlertDutyService dutyService;
    private final AlertNotifyService notifyService;
    private final SysUserMapper userMapper;
    private final CmdbHostMapper hostMapper;

    public PageResult<AlertEventVO> page(long current, long size, Integer status, Integer alertLevel, Long hostId, String keyword) {
        Page<AlertEvent> page = eventMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<AlertEvent>lambdaQuery()
                        .eq(status != null, AlertEvent::getStatus, status)
                        .eq(alertLevel != null, AlertEvent::getAlertLevel, alertLevel)
                        .eq(hostId != null, AlertEvent::getHostId, hostId)
                        .and(StringUtils.hasText(keyword), w -> w.like(AlertEvent::getMetric, keyword)
                                .or().like(AlertEvent::getCurrentValue, keyword))
                        .orderByDesc(AlertEvent::getCreateTime));
        return toPageVO(page);
    }

    public AlertEventVO detail(Long id) {
        return toVO(requireEvent(id));
    }

    /** 告警事件通知记录 */
    public List<Object> notifyLogs(Long eventId) {
        requireEvent(eventId);
        return List.of();
    }

    /** 确认告警 */
    @Transactional
    public void confirm(Long id, Long userId) {
        AlertEvent event = requireEvent(id);
        if (event.getStatus() == STATUS_CONFIRMED) {
            throw new BusinessException(ErrorCode.ALERT_EVENT_ALREADY_CONFIRMED);
        }
        event.setStatus(STATUS_CONFIRMED);
        event.setConfirmUserId(userId);
        event.setConfirmTime(LocalDateTime.now());
        eventMapper.updateById(event);
    }

    /** 静默告警（临时屏蔽，截止后回到告警中） */
    @Transactional
    public void silence(Long id, Integer silenceMinutes) {
        AlertEvent event = requireEvent(id);
        event.setStatus(STATUS_SILENCED);
        event.setSilenceUntil(LocalDateTime.now().plusMinutes(silenceMinutes));
        eventMapper.updateById(event);
    }

    /** 恢复告警（指标恢复正常后调用） */
    @Transactional
    public void recover(Long id) {
        AlertEvent event = requireEvent(id);
        if (event.getStatus() == STATUS_RECOVERED) {
            return;
        }
        event.setStatus(STATUS_RECOVERED);
        event.setRecoverTime(LocalDateTime.now());
        eventMapper.updateById(event);
    }

    /**
     * 触发告警（供指标采集/外部接入调用）
     * 告警收敛：相同规则相同主机 5 分钟内重复触发只生成一条（不重复生成）
     */
    @Transactional
    public AlertEvent trigger(Long ruleId, Long hostId, BigDecimal currentValue) {
        AlertRule rule = ruleService.getById(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        if (rule.getStatus() == 0) {
            return null; // 规则停用不触发
        }
        // 收敛：5 分钟内该规则该主机已有未恢复告警则复用
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(CONVERGENCE_MINUTES);
        AlertEvent existing = eventMapper.selectOne(
                Wrappers.<AlertEvent>lambdaQuery()
                        .eq(AlertEvent::getRuleId, ruleId)
                        .eq(AlertEvent::getHostId, hostId)
                        .in(AlertEvent::getStatus, STATUS_ACTIVE, STATUS_CONFIRMED, STATUS_SILENCED)
                        .ge(AlertEvent::getCreateTime, windowStart)
                        .orderByDesc(AlertEvent::getCreateTime)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setCurrentValue(currentValue);
            existing.setThreshold(rule.getThreshold());
            eventMapper.updateById(existing);
            return existing;
        }

        AlertEvent event = new AlertEvent();
        event.setRuleId(ruleId);
        event.setHostId(hostId);
        event.setAlertLevel(rule.getAlertLevel());
        event.setMetric(rule.getMetric());
        event.setCurrentValue(currentValue);
        event.setThreshold(rule.getThreshold());
        event.setStatus(STATUS_ACTIVE);
        event.setCreateTime(LocalDateTime.now());
        eventMapper.insert(event);

        // 通知：优先规则通知人，否则当日值班人
        List<Long> userIds = parseUserIds(rule.getNotifyUsers());
        if (userIds.isEmpty()) {
            userIds = dutyService.getDutyUserIds(LocalDate.now());
        }
        notifyService.notifyAlert(rule, event, userIds);
        return event;
    }

    /** 告警统计 */
    public AlertStatsVO stats() {
        AlertStatsVO vo = new AlertStatsVO();
        vo.setActiveAlerts(countByStatusActive());
        vo.setTodayAlerts(countToday());
        vo.setByLevel(countByLevel());
        vo.setTopHosts(topHosts(5));
        return vo;
    }

    private Long countByStatusActive() {
        return eventMapper.selectCount(
                Wrappers.<AlertEvent>lambdaQuery()
                        .in(AlertEvent::getStatus, STATUS_ACTIVE, STATUS_CONFIRMED, STATUS_SILENCED));
    }

    private Long countToday() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return eventMapper.selectCount(
                Wrappers.<AlertEvent>lambdaQuery().ge(AlertEvent::getCreateTime, start));
    }

    private Map<String, Long> countByLevel() {
        List<AlertEvent> events = eventMapper.selectList(
                Wrappers.<AlertEvent>lambdaQuery()
                        .in(AlertEvent::getStatus, STATUS_ACTIVE, STATUS_CONFIRMED, STATUS_SILENCED));
        Map<String, Long> byLevel = new LinkedHashMap<>();
        byLevel.put("urgent", 0L);
        byLevel.put("high", 0L);
        byLevel.put("medium", 0L);
        byLevel.put("low", 0L);
        for (AlertEvent e : events) {
            String key = switch (e.getAlertLevel() == null ? 3 : e.getAlertLevel()) {
                case 0 -> "urgent";
                case 1 -> "high";
                case 2 -> "medium";
                default -> "low";
            };
            byLevel.merge(key, 1L, Long::sum);
        }
        return byLevel;
    }

    private List<AlertStatsVO.HostAlertVO> topHosts(int limit) {
        Map<Long, Long> countByHost = eventMapper.selectList(null).stream()
                .filter(e -> e.getHostId() != null)
                .collect(Collectors.groupingBy(AlertEvent::getHostId, Collectors.counting()));
        return countByHost.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    AlertStatsVO.HostAlertVO h = new AlertStatsVO.HostAlertVO();
                    h.setHostId(e.getKey());
                    h.setAlertCount(e.getValue());
                    CmdbHost host = hostMapper.selectById(e.getKey());
                    if (host != null) {
                        h.setHostname(host.getHostname());
                    }
                    return h;
                })
                .collect(Collectors.toList());
    }

    private List<Long> parseUserIds(String userIds) {
        if (!StringUtils.hasText(userIds)) {
            return List.of();
        }
        return java.util.Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /** 供 Controller 返回 VO（trigger 无结果时返回空 VO） */
    public AlertEventVO toVOForController(AlertEvent event) {
        if (event == null) {
            return null;
        }
        return toVO(event);
    }

    private AlertEvent requireEvent(Long id) {
        AlertEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.ALERT_EVENT_NOT_FOUND);
        }
        return event;
    }

    private PageResult<AlertEventVO> toPageVO(Page<AlertEvent> page) {
        List<AlertEventVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private AlertEventVO toVO(AlertEvent event) {
        AlertEventVO vo = new AlertEventVO();
        vo.setId(event.getId());
        vo.setRuleId(event.getRuleId());
        AlertRule rule = ruleService.getById(event.getRuleId());
        if (rule != null) {
            vo.setRuleName(rule.getName());
        }
        vo.setHostId(event.getHostId());
        if (event.getHostId() != null) {
            CmdbHost host = hostMapper.selectById(event.getHostId());
            if (host != null) {
                vo.setHostName(host.getHostname());
                vo.setHostIp(host.getIpAddress());
            }
        }
        vo.setAlertLevel(event.getAlertLevel());
        vo.setAlertLevelName(event.getAlertLevel() != null && event.getAlertLevel() < LEVEL_NAMES.length
                ? LEVEL_NAMES[event.getAlertLevel()] : String.valueOf(event.getAlertLevel()));
        vo.setMetric(event.getMetric());
        vo.setCurrentValue(event.getCurrentValue());
        vo.setThreshold(event.getThreshold());
        vo.setStatus(event.getStatus());
        vo.setStatusName(event.getStatus() != null && event.getStatus() < STATUS_NAMES.length
                ? STATUS_NAMES[event.getStatus()] : String.valueOf(event.getStatus()));
        vo.setConfirmUserId(event.getConfirmUserId());
        if (event.getConfirmUserId() != null) {
            SysUser user = userMapper.selectById(event.getConfirmUserId());
            if (user != null) {
                vo.setConfirmUserName(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            }
        }
        vo.setConfirmTime(event.getConfirmTime());
        vo.setRecoverTime(event.getRecoverTime());
        vo.setSilenceUntil(event.getSilenceUntil());
        vo.setCreateTime(event.getCreateTime());
        return vo;
    }
}