package com.opsflow.module.cmdb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.cmdb.model.entity.CmdbService;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务资产 Mapper
 */
@Mapper
public interface CmdbServiceMapper extends BaseMapper<CmdbService> {
}