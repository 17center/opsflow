package com.opsflow.module.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工单主表 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 按编号前缀统计工单数量（含逻辑删除行，用于生成单调递增的工单编号）
     * 说明：工单采用逻辑删除，物理行仍占用 uk_ticket_no 唯一索引，
     * 故必须统计含已删除行，避免编号重复。
     */
    @Select("SELECT COUNT(*) FROM ticket WHERE ticket_no LIKE CONCAT(#{prefix}, '%')")
    Long countByNoPrefix(@Param("prefix") String prefix);
}