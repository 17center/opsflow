package com.opsflow.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.system.model.entity.SysNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内通知表 Mapper
 */
@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {
}