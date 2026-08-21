package com.opsflow.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.system.model.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统配置表 Mapper
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}