package com.opsflow.module.alert.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.alert.mapper.AlertRuleMapper;
import com.opsflow.module.alert.model.dto.AlertRuleDTO;
import com.opsflow.module.alert.model.entity.AlertRule;
import com.opsflow.module.alert.model.vo.AlertRuleVO;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警规则服务：CRUD、启停用、同指标同主机唯一性校验
 */
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private static final String[] LEVEL_NAMES = {"紧急", "高", "中", "低"};
    private static final String[] STATUS_NAMES = {"停用", "启用"};

    private final AlertRuleMapper ruleMapper;
    private final CmdbHostMapper hostMapper;

    public PageResult<AlertRuleVO> page(long current, long size, String keyword, Integer status, Long hostId) {
        Page<AlertRule> page = ruleMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<AlertRule>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w.like(AlertRule::getName, keyword)
                                .or().like(AlertRule::getMetric, keyword))
                        .eq(status != null, AlertRule::getStatus, status)
                        .eq(hostId != null, AlertRule::getHostId, hostId)
                        .orderByDesc(AlertRule::getCreateTime));
        return toPageVO(page);
    }

    public AlertRuleVO detail(Long id) {
        return toVO(requireRule(id));
    }

    @Transactional
    public void create(AlertRuleDTO dto, String operator) {
        checkUnique(dto.getHostId(), dto.getMetric(), null);
        AlertRule rule = new AlertRule();
        applyDto(rule, dto);
        rule.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        rule.setCreateBy(operator);
        ruleMapper.insert(rule);
    }

    @Transactional
    public void update(Long id, AlertRuleDTO dto, String operator) {
        AlertRule rule = requireRule(id);
        checkUnique(dto.getHostId(), dto.getMetric(), id);
        applyDto(rule, dto);
        rule.setUpdateBy(operator);
        ruleMapper.updateById(rule);
    }

    @Transactional
    public void delete(Long id) {
        requireRule(id);
        ruleMapper.deleteById(id);
    }

    @Transactional
    public void changeStatus(Long id, Integer status, String operator) {
        AlertRule rule = requireRule(id);
        rule.setStatus(status);
        rule.setUpdateBy(operator);
        ruleMapper.updateById(rule);
    }

    /** 同一指标同一主机只允许一条生效规则（hostId 为空视为全局规则） */
    private void checkUnique(Long hostId, String metric, Long excludeId) {
        Long count = ruleMapper.selectCount(
                Wrappers.<AlertRule>lambdaQuery()
                        .eq(hostId != null, AlertRule::getHostId, hostId)
                        .isNull(hostId == null, AlertRule::getHostId)
                        .eq(AlertRule::getMetric, metric)
                        .ne(excludeId != null, AlertRule::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ALERT_RULE_EXISTS);
        }
    }

    private void applyDto(AlertRule rule, AlertRuleDTO dto) {
        rule.setName(dto.getName());
        rule.setHostId(dto.getHostId());
        rule.setMetric(dto.getMetric());
        rule.setOperator(dto.getOperator());
        rule.setThreshold(dto.getThreshold());
        rule.setDurationSeconds(dto.getDurationSeconds() == null ? 60 : dto.getDurationSeconds());
        rule.setAlertLevel(dto.getAlertLevel());
        rule.setNotifyChannels(dto.getNotifyChannels());
        rule.setNotifyUsers(dto.getNotifyUsers());
        rule.setRemark(dto.getRemark());
    }

    private AlertRule requireRule(Long id) {
        AlertRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }
        return rule;
    }

    private PageResult<AlertRuleVO> toPageVO(Page<AlertRule> page) {
        List<AlertRuleVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private AlertRuleVO toVO(AlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        vo.setId(rule.getId());
        vo.setName(rule.getName());
        vo.setHostId(rule.getHostId());
        if (rule.getHostId() != null) {
            CmdbHost host = hostMapper.selectById(rule.getHostId());
            if (host != null) {
                vo.setHostName(host.getHostname());
            }
        }
        vo.setMetric(rule.getMetric());
        vo.setOperator(rule.getOperator());
        vo.setThreshold(rule.getThreshold());
        vo.setDurationSeconds(rule.getDurationSeconds());
        vo.setAlertLevel(rule.getAlertLevel());
        vo.setAlertLevelName(rule.getAlertLevel() != null && rule.getAlertLevel() < LEVEL_NAMES.length
                ? LEVEL_NAMES[rule.getAlertLevel()] : String.valueOf(rule.getAlertLevel()));
        vo.setNotifyChannels(rule.getNotifyChannels());
        vo.setNotifyUsers(rule.getNotifyUsers());
        vo.setStatus(rule.getStatus());
        vo.setStatusName(rule.getStatus() != null && rule.getStatus() < STATUS_NAMES.length
                ? STATUS_NAMES[rule.getStatus()] : String.valueOf(rule.getStatus()));
        vo.setCreateTime(rule.getCreateTime());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    /** 供事件触发使用的规则查询 */
    public AlertRule getById(Long id) {
        return ruleMapper.selectById(id);
    }
}