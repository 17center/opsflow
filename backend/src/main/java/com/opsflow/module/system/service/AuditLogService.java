package com.opsflow.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.system.mapper.SysAuditLogMapper;
import com.opsflow.module.system.model.entity.SysAuditLog;
import com.opsflow.module.system.model.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 分页查询审计日志（可按模块/操作人/操作结果/时间范围过滤）
     */
    public PageResult<AuditLogVO> page(long current, long size, String username, String module,
                                       Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<SysAuditLog>()
                .like(StringUtils.hasText(username), SysAuditLog::getUsername, username)
                .eq(StringUtils.hasText(module), SysAuditLog::getModule, module)
                .eq(status != null, SysAuditLog::getStatus, status)
                .ge(startTime != null, SysAuditLog::getCreateTime, startTime)
                .le(endTime != null, SysAuditLog::getCreateTime, endTime)
                .orderByDesc(SysAuditLog::getCreateTime)
                .orderByDesc(SysAuditLog::getId);

        Page<SysAuditLog> page = auditLogMapper.selectPage(new Page<>(current, size), wrapper);
        List<AuditLogVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 清空指定时间之前的审计日志（数据保留策略）
     */
    public void cleanBefore(LocalDateTime beforeTime) {
        int deleted = auditLogMapper.delete(
                new LambdaQueryWrapper<SysAuditLog>().lt(SysAuditLog::getCreateTime, beforeTime));
        log.info("清理审计日志: beforeTime={}, deleted={}", beforeTime, deleted);
    }

    private AuditLogVO toVO(SysAuditLog entity) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}