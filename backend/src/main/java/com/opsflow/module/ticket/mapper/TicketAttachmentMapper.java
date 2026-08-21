package com.opsflow.module.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.ticket.model.entity.TicketAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单附件表 Mapper
 */
@Mapper
public interface TicketAttachmentMapper extends BaseMapper<TicketAttachment> {
}