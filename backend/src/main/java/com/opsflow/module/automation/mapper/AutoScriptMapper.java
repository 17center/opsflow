package com.opsflow.module.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.automation.model.entity.AutoScript;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化脚本 Mapper
 */
@Mapper
public interface AutoScriptMapper extends BaseMapper<AutoScript> {
}