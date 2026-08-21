package com.opsflow.module.cmdb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.cmdb.model.entity.CmdbRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产关联关系 Mapper
 */
@Mapper
public interface CmdbRelationMapper extends BaseMapper<CmdbRelation> {
}