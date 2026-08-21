package com.opsflow.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.alert.model.entity.AlertDutySchedule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 值班排班 Mapper
 */
@Mapper
public interface AlertDutyScheduleMapper extends BaseMapper<AlertDutySchedule> {
}