package com.opsflow.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.alert.model.entity.AlertEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警事件 Mapper
 */
@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEvent> {
}