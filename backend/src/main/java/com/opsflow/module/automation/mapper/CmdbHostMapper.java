package com.opsflow.module.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.automation.model.entity.CmdbHost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 目标主机 Mapper
 */
@Mapper
public interface CmdbHostMapper extends BaseMapper<CmdbHost> {
}