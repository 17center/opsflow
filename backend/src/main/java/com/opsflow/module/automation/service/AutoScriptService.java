package com.opsflow.module.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.common.util.AesUtil;
import com.opsflow.module.automation.mapper.AutoScriptMapper;
import com.opsflow.module.automation.mapper.AutoScriptVersionMapper;
import com.opsflow.module.automation.model.dto.AutoScriptDTO;
import com.opsflow.module.automation.model.entity.AutoScript;
import com.opsflow.module.automation.model.entity.AutoScriptVersion;
import com.opsflow.module.automation.model.vo.AutoScriptVO;
import com.opsflow.module.automation.model.vo.AutoScriptVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动化脚本服务：CRUD、版本管理与回滚、启停用
 */
@Service
@RequiredArgsConstructor
public class AutoScriptService {

    private static final String[] SCRIPT_TYPE_NAMES = {"", "Shell", "Python", "Ansible"};

    private final AutoScriptMapper scriptMapper;
    private final AutoScriptVersionMapper versionMapper;

    public PageResult<AutoScriptVO> page(long current, long size, String keyword, Integer scriptType, Integer status) {
        Page<AutoScript> page = scriptMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<AutoScript>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w.like(AutoScript::getName, keyword)
                                .or().like(AutoScript::getCategory, keyword))
                        .eq(scriptType != null, AutoScript::getScriptType, scriptType)
                        .eq(status != null, AutoScript::getStatus, status)
                        .orderByDesc(AutoScript::getCreateTime));
        List<AutoScriptVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    public AutoScriptVO detail(Long id) {
        AutoScript script = requireScript(id);
        return toVO(script);
    }

    @Transactional
    public void create(AutoScriptDTO dto, String operator) {
        AutoScript script = new AutoScript();
        script.setName(dto.getName());
        script.setDescription(dto.getDescription());
        script.setScriptType(dto.getScriptType());
        script.setContent(AesUtil.encrypt(dto.getContent()));
        script.setParamsSchema(dto.getParamsSchema());
        script.setTimeoutSeconds(dto.getTimeoutSeconds());
        script.setCategory(dto.getCategory());
        script.setCurrentVersion(1);
        script.setStatus(1);
        script.setCreateBy(operator);
        scriptMapper.insert(script);

        saveVersion(script.getId(), 1, dto.getContent(), dto.getChangeLog(), operator);
    }

    @Transactional
    public void update(Long id, AutoScriptDTO dto, String operator) {
        AutoScript script = requireScript(id);
        int newVersion = (script.getCurrentVersion() == null ? 0 : script.getCurrentVersion()) + 1;

        script.setName(dto.getName());
        script.setDescription(dto.getDescription());
        script.setScriptType(dto.getScriptType());
        script.setContent(AesUtil.encrypt(dto.getContent()));
        script.setParamsSchema(dto.getParamsSchema());
        script.setTimeoutSeconds(dto.getTimeoutSeconds());
        script.setCategory(dto.getCategory());
        script.setCurrentVersion(newVersion);
        script.setUpdateBy(operator);
        scriptMapper.updateById(script);

        saveVersion(id, newVersion, dto.getContent(), dto.getChangeLog(), operator);
    }

    @Transactional
    public void delete(Long id) {
        requireScript(id);
        scriptMapper.deleteById(id);
        // 版本表外键 CASCADE 物理删除，此处逻辑删除脚本即可
    }

    @Transactional
    public void changeStatus(Long id, Integer status, String operator) {
        AutoScript script = requireScript(id);
        script.setStatus(status);
        script.setUpdateBy(operator);
        scriptMapper.updateById(script);
    }

    public List<AutoScriptVersionVO> versions(Long scriptId) {
        requireScript(scriptId);
        return versionMapper.selectList(
                        Wrappers.<AutoScriptVersion>lambdaQuery()
                                .eq(AutoScriptVersion::getScriptId, scriptId)
                                .orderByDesc(AutoScriptVersion::getVersion))
                .stream().map(v -> {
                    AutoScriptVersionVO vo = new AutoScriptVersionVO();
                    vo.setId(v.getId());
                    vo.setScriptId(v.getScriptId());
                    vo.setVersion(v.getVersion());
                    vo.setContent(AesUtil.decrypt(v.getContent()));
                    vo.setChangeLog(v.getChangeLog());
                    vo.setCreateBy(v.getCreateBy());
                    vo.setCreateTime(v.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());
    }

    @Transactional
    public void rollback(Long id, Integer version, String operator) {
        AutoScript script = requireScript(id);
        AutoScriptVersion v = versionMapper.selectOne(
                Wrappers.<AutoScriptVersion>lambdaQuery()
                        .eq(AutoScriptVersion::getScriptId, id)
                        .eq(AutoScriptVersion::getVersion, version));
        if (v == null) {
            throw new BusinessException(ErrorCode.SCRIPT_VERSION_NOT_FOUND);
        }
        String content = AesUtil.decrypt(v.getContent());
        int newVersion = (script.getCurrentVersion() == null ? 0 : script.getCurrentVersion()) + 1;

        script.setContent(AesUtil.encrypt(content));
        script.setCurrentVersion(newVersion);
        script.setUpdateBy(operator);
        scriptMapper.updateById(script);

        saveVersion(id, newVersion, content, "回滚至 v" + version, operator);
    }

    private void saveVersion(Long scriptId, int version, String plainContent, String changeLog, String operator) {
        AutoScriptVersion v = new AutoScriptVersion();
        v.setScriptId(scriptId);
        v.setVersion(version);
        v.setContent(AesUtil.encrypt(plainContent));
        v.setChangeLog(changeLog);
        v.setCreateBy(operator);
        versionMapper.insert(v);
    }

    private AutoScript requireScript(Long id) {
        AutoScript script = scriptMapper.selectById(id);
        if (script == null) {
            throw new BusinessException(ErrorCode.SCRIPT_NOT_FOUND);
        }
        return script;
    }

    private AutoScriptVO toVO(AutoScript script) {
        AutoScriptVO vo = new AutoScriptVO();
        vo.setId(script.getId());
        vo.setName(script.getName());
        vo.setDescription(script.getDescription());
        vo.setScriptType(script.getScriptType());
        vo.setScriptTypeName(script.getScriptType() == null || script.getScriptType() >= SCRIPT_TYPE_NAMES.length
                ? String.valueOf(script.getScriptType()) : SCRIPT_TYPE_NAMES[script.getScriptType()]);
        vo.setContent(AesUtil.decrypt(script.getContent()));
        vo.setParamsSchema(script.getParamsSchema());
        vo.setTimeoutSeconds(script.getTimeoutSeconds());
        vo.setCurrentVersion(script.getCurrentVersion());
        vo.setStatus(script.getStatus());
        vo.setStatusName(script.getStatus() != null && script.getStatus() == 1 ? "启用" : "停用");
        vo.setCategory(script.getCategory());
        vo.setCreateTime(script.getCreateTime());
        vo.setUpdateTime(script.getUpdateTime());
        return vo;
    }
}