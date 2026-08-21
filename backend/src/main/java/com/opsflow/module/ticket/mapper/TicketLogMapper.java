package com.opsflow.module.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.ticket.model.entity.TicketLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单状态变更日志表 Mapper
 */
@Mapper
public interface TicketLogMapper extends BaseMapper<TicketLog> {
}