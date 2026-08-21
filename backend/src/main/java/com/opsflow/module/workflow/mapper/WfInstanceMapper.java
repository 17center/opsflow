package com.opsflow.module.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.workflow.model.entity.WfInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程实例表 Mapper
 */
@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {
}