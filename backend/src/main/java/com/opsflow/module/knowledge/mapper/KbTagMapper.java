package com.opsflow.module.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.knowledge.model.entity.KbTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper
 */
@Mapper
public interface KbTagMapper extends BaseMapper<KbTag> {
}