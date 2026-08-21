package com.opsflow.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.alert.model.entity.AlertRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则 Mapper
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule> {
}