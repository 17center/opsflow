package com.opsflow.module.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.knowledge.mapper.KbArticleTagMapper;
import com.opsflow.module.knowledge.mapper.KbTagMapper;
import com.opsflow.module.knowledge.model.dto.KbTagDTO;
import com.opsflow.module.knowledge.model.entity.KbTag;
import com.opsflow.module.knowledge.model.vo.KbTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务：列表、创建、删除
 */
@Service
@RequiredArgsConstructor
public class KbTagService {

    private final KbTagMapper tagMapper;
    private final KbArticleTagMapper articleTagMapper;

    public List<KbTagVO> list() {
        return tagMapper.selectList(Wrappers.<KbTag>lambdaQuery().orderByAsc(KbTag::getId))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Transactional
    public void create(KbTagDTO dto) {
        Long count = tagMapper.selectCount(
                Wrappers.<KbTag>lambdaQuery().eq(KbTag::getName, dto.getName()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.KB_TAG_EXISTS);
        }
        KbTag tag = new KbTag();
        tag.setName(dto.getName());
        tagMapper.insert(tag);
    }

    @Transactional
    public void delete(Long id) {
        requireTag(id);
        Long refCount = articleTagMapper.selectCount(
                Wrappers.<com.opsflow.module.knowledge.model.entity.KbArticleTag>lambdaQuery()
                        .eq(com.opsflow.module.knowledge.model.entity.KbArticleTag::getTagId, id));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(ErrorCode.KB_TAG_HAS_ARTICLES);
        }
        tagMapper.deleteById(id);
    }

    private KbTag requireTag(Long id) {
        KbTag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.KB_TAG_NOT_FOUND);
        }
        return tag;
    }

    private KbTagVO toVO(KbTag tag) {
        KbTagVO vo = new KbTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCreateTime(tag.getCreateTime());
        return vo;
    }
}