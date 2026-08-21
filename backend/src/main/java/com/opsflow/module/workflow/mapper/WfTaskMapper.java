package com.opsflow.module.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.workflow.model.entity.WfTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批任务表 Mapper
 */
@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {
}