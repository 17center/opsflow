package com.opsflow.module.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.ticket.model.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单评论表 Mapper
 */
@Mapper
public interface TicketCommentMapper extends BaseMapper<TicketComment> {
}