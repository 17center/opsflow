package com.opsflow.module.ticket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.module.system.service.NotificationService;
import com.opsflow.module.ticket.mapper.TicketCommentMapper;
import com.opsflow.module.ticket.mapper.TicketLogMapper;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.dto.TicketCommentDTO;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.entity.TicketComment;
import com.opsflow.module.ticket.model.entity.TicketLog;
import com.opsflow.module.ticket.model.vo.CommentVO;
import com.opsflow.module.ticket.model.vo.UserRefVO;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单评论服务：发表评论（Markdown、@提及）与评论列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCommentService {

    private final TicketMapper ticketMapper;
    private final TicketCommentMapper commentMapper;
    private final TicketLogMapper ticketLogMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 发表评论，@ 的用户收到站内通知，并记录评论日志
     */
    @Transactional
    public void add(Long ticketId, TicketCommentDTO dto, Long operatorId, String operatorName) {
        Ticket ticket = getTicket(ticketId);
        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(operatorId);
        comment.setContent(dto.getContent());
        if (dto.getMentionedUserIds() != null && !dto.getMentionedUserIds().isEmpty()) {
            comment.setMentionedUserIds(dto.getMentionedUserIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        comment.setCreateBy(operatorName);
        commentMapper.insert(comment);

        // 记录评论日志
        TicketLog ticketLog = new TicketLog();
        ticketLog.setTicketId(ticketId);
        ticketLog.setAction("COMMENT");
        ticketLog.setOperatorId(operatorId);
        ticketLog.setContent(dto.getContent());
        ticketLogMapper.insert(ticketLog);

        // 通知被 @ 的用户
        if (dto.getMentionedUserIds() != null) {
            for (Long userId : dto.getMentionedUserIds()) {
                if (userId.equals(operatorId)) {
                    continue;
                }
                notificationService.notify(userId, "工单评论 @ 提醒",
                        operatorName + " 在工单 " + ticket.getTicketNo() + " 中评论并提及了您",
                        1, ticketId, "TICKET");
            }
        }
        log.info("工单评论: ticketId={}, by={}", ticketId, operatorId);
    }

    /**
     * 工单评论列表（按时间正序）
     */
    public List<CommentVO> list(Long ticketId) {
        getTicket(ticketId);
        List<TicketComment> comments = commentMapper.selectList(
                Wrappers.<TicketComment>lambdaQuery()
                        .eq(TicketComment::getTicketId, ticketId)
                        .orderByAsc(TicketComment::getId));
        List<Long> userIds = comments.stream().map(TicketComment::getUserId).distinct().toList();
        Map<Long, String> nameMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(userIds);
            nameMap = users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
        }
        final Map<Long, String> finalNameMap = nameMap;
        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setContent(c.getContent());
            vo.setCreateTime(c.getCreateTime());
            UserRefVO user = new UserRefVO();
            user.setId(c.getUserId());
            user.setNickname(finalNameMap.getOrDefault(c.getUserId(), String.valueOf(c.getUserId())));
            vo.setUser(user);
            return vo;
        }).toList();
    }

    private Ticket getTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "工单不存在");
        }
        return ticket;
    }
}