package com.opsflow.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.knowledge.model.entity.KbArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章-标签关联 Mapper
 */
@Mapper
public interface KbArticleTagMapper extends BaseMapper<KbArticleTag> {
}