package com.opsflow.module.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.automation.model.entity.AutoExecLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行输出日志 Mapper
 */
@Mapper
public interface AutoExecLogMapper extends BaseMapper<AutoExecLog> {
}