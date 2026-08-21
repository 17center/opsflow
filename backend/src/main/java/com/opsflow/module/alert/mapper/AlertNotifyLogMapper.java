package com.opsflow.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.alert.model.entity.AlertNotifyLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警通知记录 Mapper
 */
@Mapper
public interface AlertNotifyLogMapper extends BaseMapper<AlertNotifyLog> {
}