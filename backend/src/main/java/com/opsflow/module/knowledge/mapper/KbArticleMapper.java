package com.opsflow.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.knowledge.model.entity.KbArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识文章 Mapper
 */
@Mapper
public interface KbArticleMapper extends BaseMapper<KbArticle> {
}