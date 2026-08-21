package com.opsflow.module.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.common.util.AesUtil;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.dto.CmdbHostDTO;
import com.opsflow.module.automation.model.entity.CmdbHost;
import com.opsflow.module.automation.model.vo.CmdbHostVO;
import com.opsflow.module.automation.ssh.SshExecutionException;
import com.opsflow.module.automation.ssh.SshExecutor;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 目标主机服务：CRUD、连接测试
 */
@Service
@RequiredArgsConstructor
public class CmdbHostService {

    private static final String[] STATUS_NAMES = {"不可用", "运行中", "维护中", "已退役"};

    private final CmdbHostMapper hostMapper;
    private final SysUserMapper userMapper;
    private final SshExecutor sshExecutor;

    public PageResult<CmdbHostVO> page(long current, long size, String keyword, Integer status, String groupName) {
        Page<CmdbHost> page = hostMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<CmdbHost>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w.like(CmdbHost::getHostname, keyword)
                                .or().like(CmdbHost::getIpAddress, keyword))
                        .eq(status != null, CmdbHost::getStatus, status)
                        .eq(StringUtils.hasText(groupName), CmdbHost::getGroupName, groupName)
                        .orderByDesc(CmdbHost::getCreateTime));
        return toPageVO(page);
    }

    public CmdbHostVO detail(Long id) {
        CmdbHost host = requireHost(id);
        String ownerName = host.getOwnerId() == null ? null : resolveOwnerName(host.getOwnerId());
        return toVO(host, ownerName);
    }

    @Transactional
    public void create(CmdbHostDTO dto, String operator) {
        CmdbHost host = new CmdbHost();
        applyDto(host, dto);
        host.setStatus(1);
        host.setCreateBy(operator);
        hostMapper.insert(host);
    }

    @Transactional
    public void update(Long id, CmdbHostDTO dto, String operator) {
        CmdbHost host = requireHost(id);
        applyDto(host, dto);
        host.setUpdateBy(operator);
        hostMapper.updateById(host);
    }

    @Transactional
    public void delete(Long id) {
        requireHost(id);
        hostMapper.deleteById(id);
    }

    @Transactional
    public void changeStatus(Long id, Integer status, String operator) {
        CmdbHost host = requireHost(id);
        host.setStatus(status);
        host.setUpdateBy(operator);
        hostMapper.updateById(host);
    }

    /** 连接测试：SSH 执行 id 命令验证连通性与认证 */
    public String testConnection(Long id) {
        CmdbHost host = requireHost(id);
        try {
            String sshUser = StringUtils.hasText(host.getSshUser()) ? host.getSshUser() : "root";
            int exit = sshExecutor.execute(host.getIpAddress(), host.getSshPort(), sshUser,
                    host.getAuthType(), AesUtil.decrypt(host.getCredential()),
                    "echo opsflow-ok", 15, (t, line) -> {
                    });
            // 连接成功更新最后检查时间
            host.setLastCheckTime(LocalDateTime.now());
            hostMapper.updateById(host);
            return exit == 0 ? "连接成功" : "连接成功但在线命令返回异常";
        } catch (SshExecutionException e) {
            throw new BusinessException(ErrorCode.HOST_UNREACHABLE, e.getMessage());
        }
    }

    private void applyDto(CmdbHost host, CmdbHostDTO dto) {
        host.setHostname(dto.getHostname());
        host.setIpAddress(dto.getIpAddress());
        host.setSshPort(dto.getSshPort());
        host.setSshUser(dto.getSshUser());
        host.setOsType(dto.getOsType());
        host.setOsVersion(dto.getOsVersion());
        host.setCpuCores(dto.getCpuCores());
        host.setMemoryGb(dto.getMemoryGb());
        host.setDiskGb(dto.getDiskGb());
        host.setAuthType(dto.getAuthType());
        host.setCredential(AesUtil.encrypt(dto.getCredential()));
        host.setOwnerId(dto.getOwnerId());
        host.setGroupName(dto.getGroupName());
        host.setRemark(dto.getRemark());
    }

    private CmdbHost requireHost(Long id) {
        CmdbHost host = hostMapper.selectById(id);
        if (host == null) {
            throw new BusinessException(ErrorCode.HOST_NOT_FOUND);
        }
        return host;
    }

    private PageResult<CmdbHostVO> toPageVO(Page<CmdbHost> page) {
        Map<Long, String> ownerNames = resolveOwnerNames(page.getRecords());
        List<CmdbHostVO> records = page.getRecords().stream()
                .map(h -> toVO(h, h.getOwnerId() == null ? null : ownerNames.get(h.getOwnerId())))
                .collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private Map<Long, String> resolveOwnerNames(List<CmdbHost> records) {
        List<Long> ids = records.stream().map(CmdbHost::getOwnerId).filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername()));
    }

    private String resolveOwnerName(Long userId) {
        SysUser u = userMapper.selectById(userId);
        return u == null ? null : (StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
    }

    private CmdbHostVO toVO(CmdbHost h, String ownerName) {
        CmdbHostVO vo = new CmdbHostVO();
        vo.setId(h.getId());
        vo.setHostname(h.getHostname());
        vo.setIpAddress(h.getIpAddress());
        vo.setSshPort(h.getSshPort());
        vo.setSshUser(h.getSshUser());
        vo.setOsType(h.getOsType());
        vo.setOsVersion(h.getOsVersion());
        vo.setCpuCores(h.getCpuCores());
        vo.setMemoryGb(h.getMemoryGb());
        vo.setDiskGb(h.getDiskGb());
        vo.setAuthType(h.getAuthType());
        vo.setAuthTypeName(h.getAuthType() != null && h.getAuthType() == 2 ? "密钥" : "密码");
        vo.setStatus(h.getStatus());
        vo.setStatusName(h.getStatus() != null && h.getStatus() < STATUS_NAMES.length ? STATUS_NAMES[h.getStatus()] : String.valueOf(h.getStatus()));
        vo.setOwnerId(h.getOwnerId());
        vo.setOwnerName(ownerName);
        vo.setGroupName(h.getGroupName());
        vo.setLastCheckTime(h.getLastCheckTime());
        vo.setCreateTime(h.getCreateTime());
        vo.setRemark(h.getRemark());
        return vo;
    }
}