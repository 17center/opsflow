package com.opsflow.module.cmdb.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.automation.mapper.CmdbHostMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import com.opsflow.module.cmdb.mapper.CmdbRelationMapper;
import com.opsflow.module.cmdb.mapper.CmdbServiceMapper;
import com.opsflow.module.cmdb.model.dto.CmdbRelationDTO;
import com.opsflow.module.cmdb.model.entity.CmdbRelation;
import com.opsflow.module.cmdb.model.entity.CmdbService;
import com.opsflow.module.cmdb.model.vo.CmdbRelationVO;
import com.opsflow.module.cmdb.model.vo.CmdbTopologyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产关联服务：关系管理、拓扑图构建
 */
@Service
@RequiredArgsConstructor
public class CmdbRelationService {

    private static final Map<String, String> RELATION_TYPE_NAMES = Map.of(
            "DEPLOYED_ON", "部署于",
            "DEPENDS_ON", "依赖",
            "CONTAINS", "包含");

    private static final Map<String, String> ASSET_TYPE_NAMES = Map.of(
            "HOST", "主机",
            "SERVICE", "服务");

    private final CmdbRelationMapper relationMapper;
    private final CmdbHostMapper hostMapper;
    private final CmdbServiceMapper serviceMapper;

    /** 关联关系列表 */
    public List<CmdbRelationVO> list() {
        return relationMapper.selectList(
                        Wrappers.<CmdbRelation>lambdaQuery().orderByDesc(CmdbRelation::getCreateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public void create(CmdbRelationDTO dto, String operator) {
        // 校验资产存在
        validateAsset(dto.getSourceType(), dto.getSourceId());
        validateAsset(dto.getTargetType(), dto.getTargetId());
        // 不能自关联
        if (dto.getSourceType().equals(dto.getTargetType()) && dto.getSourceId().equals(dto.getTargetId())) {
            throw new BusinessException(ErrorCode.RELATION_SELF);
        }
        // 防重复
        Long exists = relationMapper.selectCount(
                Wrappers.<CmdbRelation>lambdaQuery()
                        .eq(CmdbRelation::getSourceType, dto.getSourceType())
                        .eq(CmdbRelation::getSourceId, dto.getSourceId())
                        .eq(CmdbRelation::getTargetType, dto.getTargetType())
                        .eq(CmdbRelation::getTargetId, dto.getTargetId())
                        .eq(CmdbRelation::getRelationType, dto.getRelationType()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.RELATION_EXISTS);
        }
        CmdbRelation relation = new CmdbRelation();
        relation.setSourceType(dto.getSourceType());
        relation.setSourceId(dto.getSourceId());
        relation.setTargetType(dto.getTargetType());
        relation.setTargetId(dto.getTargetId());
        relation.setRelationType(dto.getRelationType());
        relation.setCreateBy(operator);
        relationMapper.insert(relation);
    }

    @Transactional
    public void delete(Long id) {
        CmdbRelation relation = relationMapper.selectById(id);
        if (relation == null) {
            throw new BusinessException(ErrorCode.RELATION_NOT_FOUND);
        }
        relationMapper.deleteById(id);
    }

    /** 构建资产拓扑图（全部主机 + 服务节点及关系边） */
    public CmdbTopologyVO topology() {
        List<CmdbHost> hosts = hostMapper.selectList(null);
        List<CmdbService> services = serviceMapper.selectList(null);
        List<CmdbRelation> relations = relationMapper.selectList(null);

        CmdbTopologyVO vo = new CmdbTopologyVO();
        List<CmdbTopologyVO.Node> nodes = new ArrayList<>();
        List<CmdbTopologyVO.Edge> edges = new ArrayList<>();
        Map<String, CmdbTopologyVO.Node> nodeMap = new HashMap<>();

        // 主机节点
        for (CmdbHost host : hosts) {
            CmdbTopologyVO.Node node = new CmdbTopologyVO.Node();
            node.setId("HOST-" + host.getId());
            node.setType("HOST");
            node.setName(host.getHostname());
            node.setLabel(host.getHostname() + "\n" + host.getIpAddress());
            node.setStatus(host.getStatus());
            nodes.add(node);
            nodeMap.put(node.getId(), node);
        }
        // 服务节点
        for (CmdbService service : services) {
            CmdbTopologyVO.Node node = new CmdbTopologyVO.Node();
            node.setId("SERVICE-" + service.getId());
            node.setType("SERVICE");
            node.setName(service.getName());
            node.setLabel(service.getName() + "\n" + service.getServiceType() + ":" + (service.getPort() == null ? "-" : service.getPort()));
            node.setStatus(service.getStatus());
            node.setHostId(service.getHostId());
            nodes.add(node);
            nodeMap.put(node.getId(), node);
            // 服务部署于主机 → DEPLOYED_ON 边
            if (service.getHostId() != null) {
                CmdbTopologyVO.Edge deployEdge = new CmdbTopologyVO.Edge();
                deployEdge.setSource("SERVICE-" + service.getId());
                deployEdge.setTarget("HOST-" + service.getHostId());
                deployEdge.setRelationType("DEPLOYED_ON");
                edges.add(deployEdge);
            }
        }
        // 显式关联边
        for (CmdbRelation relation : relations) {
            String src = relation.getSourceType() + "-" + relation.getSourceId();
            String tgt = relation.getTargetType() + "-" + relation.getTargetId();
            if (nodeMap.containsKey(src) && nodeMap.containsKey(tgt)) {
                CmdbTopologyVO.Edge edge = new CmdbTopologyVO.Edge();
                edge.setSource(src);
                edge.setTarget(tgt);
                edge.setRelationType(relation.getRelationType());
                edges.add(edge);
            }
        }

        vo.setNodes(nodes);
        vo.setEdges(edges);
        return vo;
    }

    private void validateAsset(String type, Long id) {
        if ("HOST".equals(type)) {
            if (hostMapper.selectById(id) == null) {
                throw new BusinessException(ErrorCode.RELATION_INVALID);
            }
        } else if ("SERVICE".equals(type)) {
            if (serviceMapper.selectById(id) == null) {
                throw new BusinessException(ErrorCode.RELATION_INVALID);
            }
        } else {
            throw new BusinessException(ErrorCode.RELATION_INVALID);
        }
    }

    private CmdbRelationVO toVO(CmdbRelation relation) {
        CmdbRelationVO vo = new CmdbRelationVO();
        vo.setId(relation.getId());
        vo.setSourceType(relation.getSourceType());
        vo.setSourceTypeName(ASSET_TYPE_NAMES.getOrDefault(relation.getSourceType(), relation.getSourceType()));
        vo.setSourceId(relation.getSourceId());
        vo.setSourceName(resolveName(relation.getSourceType(), relation.getSourceId()));
        vo.setTargetType(relation.getTargetType());
        vo.setTargetTypeName(ASSET_TYPE_NAMES.getOrDefault(relation.getTargetType(), relation.getTargetType()));
        vo.setTargetId(relation.getTargetId());
        vo.setTargetName(resolveName(relation.getTargetType(), relation.getTargetId()));
        vo.setRelationType(relation.getRelationType());
        vo.setRelationTypeName(RELATION_TYPE_NAMES.getOrDefault(relation.getRelationType(), relation.getRelationType()));
        vo.setCreateTime(relation.getCreateTime());
        return vo;
    }

    private String resolveName(String type, Long id) {
        if ("HOST".equals(type)) {
            CmdbHost host = hostMapper.selectById(id);
            return host == null ? ("主机#" + id) : host.getHostname();
        }
        if ("SERVICE".equals(type)) {
            CmdbService service = serviceMapper.selectById(id);
            return service == null ? ("服务#" + id) : service.getName();
        }
        return String.valueOf(id);
    }
}