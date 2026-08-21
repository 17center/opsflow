package com.opsflow.module.automation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.automation.model.entity.AutoExecRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 执行记录 Mapper
 */
@Mapper
public interface AutoExecRecordMapper extends BaseMapper<AutoExecRecord> {
}