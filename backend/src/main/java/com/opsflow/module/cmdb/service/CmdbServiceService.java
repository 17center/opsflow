package com.opsflow.module.cmdb.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.common.util.AesUtil;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import com.opsflow.module.automation.ssh.SshExecutor;
import com.opsflow.module.cmdb.mapper.CmdbServiceMapper;
import com.opsflow.module.cmdb.model.dto.CmdbServiceDTO;
import com.opsflow.module.cmdb.model.entity.CmdbService;
import com.opsflow.module.cmdb.model.vo.CmdbServiceVO;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务资产服务：CRUD、状态管理、自动发现
 */
@Service
@RequiredArgsConstructor
public class CmdbServiceService {

    private static final String[] STATUS_NAMES = {"不可用", "运行中", "维护中"};

    /** 自动发现探测的服务类型与端口 */
    private static final Map<Integer, String> DISCOVERY_PORTS = Map.of(
            3306, "MySQL",
            6379, "Redis",
            80, "Nginx",
            443, "Nginx",
            5432, "PostgreSQL",
            27017, "MongoDB",
            8080, "Tomcat",
            9200, "Elasticsearch");

    private final CmdbServiceMapper serviceMapper;
    private final CmdbHostMapper hostMapper;
    private final SysUserMapper userMapper;
    private final SshExecutor sshExecutor;

    public PageResult<CmdbServiceVO> page(long current, long size, String keyword, String serviceType, Integer status) {
        Page<CmdbService> page = serviceMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<CmdbService>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w.like(CmdbService::getName, keyword)
                                .or().like(CmdbService::getServiceType, keyword))
                        .eq(StringUtils.hasText(serviceType), CmdbService::getServiceType, serviceType)
                        .eq(status != null, CmdbService::getStatus, status)
                        .orderByDesc(CmdbService::getCreateTime));
        return toPageVO(page);
    }

    public CmdbServiceVO detail(Long id) {
        CmdbService service = requireService(id);
        return toVO(service);
    }

    @Transactional
    public void create(CmdbServiceDTO dto, String operator) {
        CmdbService service = new CmdbService();
        applyDto(service, dto);
        service.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        service.setCreateBy(operator);
        serviceMapper.insert(service);
    }

    @Transactional
    public void update(Long id, CmdbServiceDTO dto, String operator) {
        CmdbService service = requireService(id);
        applyDto(service, dto);
        service.setUpdateBy(operator);
        serviceMapper.updateById(service);
    }

    @Transactional
    public void delete(Long id) {
        requireService(id);
        serviceMapper.deleteById(id);
    }

    @Transactional
    public void changeStatus(Long id, Integer status, String operator) {
        CmdbService service = requireService(id);
        service.setStatus(status);
        service.setUpdateBy(operator);
        serviceMapper.updateById(service);
    }

    /**
     * 自动发现：SSH 连接到主机，探测常见服务端口是否开放，识别为服务资产
     * 返回探测到的服务列表（不落库，由前端确认后批量录入）
     */
    public List<DiscoveredService> autoDiscover(Long hostId) {
        CmdbHost host = hostMapper.selectById(hostId);
        if (host == null) {
            throw new BusinessException(ErrorCode.HOST_NOT_FOUND);
        }
        List<DiscoveredService> result = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : DISCOVERY_PORTS.entrySet()) {
            int port = entry.getKey();
            String type = entry.getValue();
            String cmd = "command -v ss >/dev/null 2>&1 && ss -tlnp | grep :" + port
                    + " || netstat -tlnp 2>/dev/null | grep :" + port;
            try {
                String credential = AesUtil.decrypt(host.getCredential());
            int exit = sshExecutor.execute(host.getIpAddress(), host.getSshPort(), "root",
                    host.getAuthType(), credential, cmd, 10, (t, line) -> {
                    });
                if (exit == 0) {
                    // 已存在相同主机+类型+端口的服务则跳过
                    Long exists = serviceMapper.selectCount(
                            Wrappers.<CmdbService>lambdaQuery()
                                    .eq(CmdbService::getHostId, hostId)
                                    .eq(CmdbService::getServiceType, type)
                                    .eq(CmdbService::getPort, port));
                    if (exists == null || exists == 0) {
                        DiscoveredService ds = new DiscoveredService();
                        ds.setName(host.getHostname() + "-" + type.toLowerCase());
                        ds.setServiceType(type);
                        ds.setHostId(hostId);
                        ds.setPort(port);
                        result.add(ds);
                    }
                }
            } catch (Exception ignored) {
                // 单个端口探测失败忽略
            }
        }
        return result;
    }

    /** 根据主机凭据执行探测（使用主机已存凭据） */
    private void applyDto(CmdbService service, CmdbServiceDTO dto) {
        service.setName(dto.getName());
        service.setServiceType(dto.getServiceType());
        service.setVersion(dto.getVersion());
        service.setHostId(dto.getHostId());
        if (dto.getHostId() != null && hostMapper.selectById(dto.getHostId()) == null) {
            throw new BusinessException(ErrorCode.SERVICE_HOST_NOT_FOUND);
        }
        service.setPort(dto.getPort());
        service.setOwnerId(dto.getOwnerId());
        service.setRemark(dto.getRemark());
    }

    private CmdbService requireService(Long id) {
        CmdbService service = serviceMapper.selectById(id);
        if (service == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND);
        }
        return service;
    }

    private PageResult<CmdbServiceVO> toPageVO(Page<CmdbService> page) {
        List<CmdbServiceVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private CmdbServiceVO toVO(CmdbService service) {
        CmdbServiceVO vo = new CmdbServiceVO();
        vo.setId(service.getId());
        vo.setName(service.getName());
        vo.setServiceType(service.getServiceType());
        vo.setVersion(service.getVersion());
        vo.setHostId(service.getHostId());
        if (service.getHostId() != null) {
            CmdbHost host = hostMapper.selectById(service.getHostId());
            if (host != null) {
                vo.setHostName(host.getHostname());
                vo.setHostIp(host.getIpAddress());
            }
        }
        vo.setPort(service.getPort());
        vo.setStatus(service.getStatus());
        vo.setStatusName(service.getStatus() != null && service.getStatus() < STATUS_NAMES.length
                ? STATUS_NAMES[service.getStatus()] : String.valueOf(service.getStatus()));
        vo.setOwnerId(service.getOwnerId());
        if (service.getOwnerId() != null) {
            SysUser owner = userMapper.selectById(service.getOwnerId());
            if (owner != null) {
                vo.setOwnerName(StringUtils.hasText(owner.getNickname()) ? owner.getNickname() : owner.getUsername());
            }
        }
        vo.setCreateTime(service.getCreateTime());
        vo.setRemark(service.getRemark());
        return vo;
    }

    /** 自动发现到的服务（待确认） */
    @lombok.Data
    public static class DiscoveredService {
        private String name;
        private String serviceType;
        private Long hostId;
        private Integer port;
    }
}