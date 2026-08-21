package com.opsflow.module.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.workflow.mapper.WfDefinitionMapper;
import com.opsflow.module.workflow.model.dto.WfDefinitionDTO;
import com.opsflow.module.workflow.model.dto.WfNodeDTO;
import com.opsflow.module.workflow.model.entity.WfDefinition;
import com.opsflow.module.workflow.model.vo.WfDefinitionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 流程定义服务：分页、创建、修改、发布、停用、详情
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfDefinitionService {

    /** 状态：0=草稿 1=已发布 2=已停用 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_DISABLED = 2;

    private final WfDefinitionMapper definitionMapper;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询流程定义
     */
    public PageResult<WfDefinitionVO> page(long current, long size, String key, String name, Integer status) {
        Page<WfDefinition> page = definitionMapper.selectPage(new Page<>(current, size),
                Wrappers.<WfDefinition>lambdaQuery()
                        .eq(StringUtils.hasText(key), WfDefinition::getKey, key)
                        .like(StringUtils.hasText(name), WfDefinition::getName, name)
                        .eq(status != null, WfDefinition::getStatus, status)
                        .orderByDesc(WfDefinition::getId));
        List<WfDefinitionVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 创建流程定义（初始为草稿，版本 1）
     */
    @Transactional
    public void create(WfDefinitionDTO dto, String operator) {
        Long count = definitionMapper.countByKey(dto.getKey());
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.WF_KEY_EXISTS);
        }
        WfDefinition definition = new WfDefinition();
        applyDto(definition, dto);
        definition.setVersion(1);
        definition.setStatus(STATUS_DRAFT);
        definition.setCreateBy(operator);
        definitionMapper.insert(definition);
        log.info("新增流程定义: key={}, by={}", dto.getKey(), operator);
    }

    /**
     * 修改流程定义（仅草稿/停用可修改，已发布不可修改）
     */
    @Transactional
    public void update(Long id, WfDefinitionDTO dto, String operator) {
        WfDefinition definition = getById(id);
        if (Objects.equals(definition.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ErrorCode.WF_PUBLISHED_NOT_EDIT);
        }
        applyDto(definition, dto);
        definition.setUpdateBy(operator);
        definitionMapper.updateById(definition);
    }

    /**
     * 发布流程定义
     */
    @Transactional
    public void publish(Long id, String operator) {
        WfDefinition definition = getById(id);
        if (Objects.equals(definition.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ErrorCode.WF_ALREADY_PUBLISHED);
        }
        definition.setStatus(STATUS_PUBLISHED);
        definition.setUpdateBy(operator);
        definitionMapper.updateById(definition);
        log.info("发布流程定义: id={}, key={}, by={}", id, definition.getKey(), operator);
    }

    /**
     * 停用流程定义
     */
    @Transactional
    public void disable(Long id, String operator) {
        WfDefinition definition = getById(id);
        if (Objects.equals(definition.getStatus(), STATUS_DISABLED)) {
            throw new BusinessException(ErrorCode.WF_ALREADY_DISABLED);
        }
        definition.setStatus(STATUS_DISABLED);
        definition.setUpdateBy(operator);
        definitionMapper.updateById(definition);
    }

    /**
     * 流程定义详情（含节点配置）
     */
    public WfDefinitionVO detail(Long id) {
        return toVO(getById(id));
    }

    /**
     * 解析节点配置 JSON
     */
    public List<WfNodeDTO> parseNodes(String bpmnXml) {
        if (!StringUtils.hasText(bpmnXml)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(bpmnXml, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("解析流程节点配置失败: {}", e.getMessage());
            return List.of();
        }
    }

    private void applyDto(WfDefinition definition, WfDefinitionDTO dto) {
        definition.setName(dto.getName());
        definition.setKey(dto.getKey());
        definition.setDescription(dto.getDescription());
        try {
            definition.setBpmnXml(objectMapper.writeValueAsString(dto.getNodes() == null ? List.of() : dto.getNodes()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "节点配置格式错误");
        }
    }

    private WfDefinitionVO toVO(WfDefinition definition) {
        WfDefinitionVO vo = new WfDefinitionVO();
        vo.setId(definition.getId());
        vo.setName(definition.getName());
        vo.setKey(definition.getKey());
        vo.setVersion(definition.getVersion());
        vo.setStatus(definition.getStatus());
        vo.setStatusName(statusName(definition.getStatus()));
        vo.setDescription(definition.getDescription());
        vo.setNodes(parseNodes(definition.getBpmnXml()));
        vo.setCreateTime(definition.getCreateTime());
        vo.setUpdateTime(definition.getUpdateTime());
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已停用";
            default -> "-";
        };
    }

    private WfDefinition getById(Long id) {
        WfDefinition definition = definitionMapper.selectById(id);
        if (definition == null) {
            throw new BusinessException(ErrorCode.WF_NOT_FOUND);
        }
        return definition;
    }
}