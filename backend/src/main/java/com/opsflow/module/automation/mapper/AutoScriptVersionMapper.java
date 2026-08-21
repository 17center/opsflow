package com.opsflow.module.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.automation.model.entity.AutoScriptVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 脚本版本 Mapper
 */
@Mapper
public interface AutoScriptVersionMapper extends BaseMapper<AutoScriptVersion> {
}