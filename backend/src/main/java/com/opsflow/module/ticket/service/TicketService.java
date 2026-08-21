package com.opsflow.module.ticket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.system.mapper.SysConfigMapper;
import com.opsflow.module.system.model.entity.SysConfig;
import com.opsflow.module.system.service.NotificationService;
import com.opsflow.module.ticket.enums.TicketPriority;
import com.opsflow.module.ticket.enums.TicketStatus;
import com.opsflow.module.ticket.enums.TicketType;
import com.opsflow.module.ticket.mapper.TicketAttachmentMapper;
import com.opsflow.module.ticket.mapper.TicketCommentMapper;
import com.opsflow.module.ticket.mapper.TicketLogMapper;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.dto.TicketCreateDTO;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.entity.TicketAttachment;
import com.opsflow.module.ticket.model.entity.TicketComment;
import com.opsflow.module.ticket.model.entity.TicketLog;
import com.opsflow.module.ticket.model.vo.AttachmentVO;
import com.opsflow.module.ticket.model.vo.CommentVO;
import com.opsflow.module.ticket.model.vo.DashboardVO;
import com.opsflow.module.ticket.model.vo.LogVO;
import com.opsflow.module.ticket.model.vo.TicketDetailVO;
import com.opsflow.module.ticket.model.vo.TicketVO;
import com.opsflow.module.ticket.model.vo.UserRefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工单服务：创建、编号生成、状态机流转、SLA、详情、看板统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String ADMIN_USERNAME = "admin";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 超级管理员账号，拥有全部工单操作权限 */
    private static final Set<String> ALLOWED_OPERATORS = Set.of("admin");

    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final TicketCommentMapper commentMapper;
    private final TicketAttachmentMapper attachmentMapper;
    private final SysUserMapper userMapper;
    private final SysConfigMapper configMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * 创建工单：生成编号、计算 SLA 截止时间、记录创建日志
     */
    @Transactional
    public Ticket create(TicketCreateDTO dto, Long operatorId, String operatorName) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo(dto.getTicketType()));
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setTicketType(dto.getTicketType());
        ticket.setPriority(dto.getPriority());
        ticket.setStatus(TicketStatus.DRAFT.getCode());
        ticket.setCreatorId(operatorId);
        ticket.setTemplateId(dto.getTemplateId());
        ticket.setHostId(dto.getHostId());
        ticket.setScriptId(dto.getScriptId());
        if (dto.getScriptParams() != null) {
            try {
                ticket.setScriptParams(objectMapper.writeValueAsString(dto.getScriptParams()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "脚本参数格式错误");
            }
        }
        // SLA 截止时间（按优先级）
        LocalDateTime now = LocalDateTime.now();
        ticket.setSlaResponseDeadline(now.plusSeconds(slaSeconds("sla.response", dto.getPriority())));
        ticket.setSlaDeadline(now.plusSeconds(slaSeconds("sla.resolve", dto.getPriority())));
        ticket.setSlaBreached(0);
        ticket.setCreateBy(operatorName);
        ticketMapper.insert(ticket);

        recordLog(ticket.getId(), "CREATE", null, ticket.getStatus(),
                operatorId, "创建工单");
        return ticket;
    }

    /**
     * 工单列表（分页 + 多条件筛选）
     */
    public PageResult<TicketVO> page(long current, long size, String keyword, String status,
                                     Integer priority, Integer ticketType, Long creatorId,
                                     Long assigneeId, LocalDateTime startTime, LocalDateTime endTime) {
        Page<Ticket> page = ticketMapper.selectPage(new Page<>(current, size),
                Wrappers.<Ticket>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Ticket::getTitle, keyword)
                                .or()
                                .likeRight(Ticket::getTicketNo, keyword))
                        .eq(StringUtils.hasText(status), Ticket::getStatus, status)
                        .eq(priority != null, Ticket::getPriority, priority)
                        .eq(ticketType != null, Ticket::getTicketType, ticketType)
                        .eq(creatorId != null, Ticket::getCreatorId, creatorId)
                        .eq(assigneeId != null, Ticket::getAssigneeId, assigneeId)
                        .ge(startTime != null, Ticket::getCreateTime, startTime)
                        .le(endTime != null, Ticket::getCreateTime, endTime)
                        .orderByDesc(Ticket::getId));
        Map<Long, String> nameMap = loadUserNames(page.getRecords(),
                Ticket::getCreatorId, Ticket::getAssigneeId);
        List<TicketVO> records = page.getRecords().stream()
                .map(t -> toVO(t, nameMap))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 工单详情（含评论、附件、操作日志）
     */
    public TicketDetailVO detail(Long id) {
        Ticket ticket = getById(id);
        TicketDetailVO vo = new TicketDetailVO();
        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setTitle(ticket.getTitle());
        vo.setDescription(ticket.getDescription());
        vo.setTicketType(ticket.getTicketType());
        vo.setTicketTypeName(TicketType.nameOf(ticket.getTicketType()));
        vo.setPriority(ticket.getPriority());
        vo.setPriorityName(TicketPriority.nameOf(ticket.getPriority()));
        vo.setStatus(ticket.getStatus());
        vo.setStatusName(TicketStatus.nameOf(ticket.getStatus()));
        vo.setHostId(ticket.getHostId());
        vo.setScriptId(ticket.getScriptId());
        vo.setScriptParams(parseParams(ticket.getScriptParams()));
        vo.setWfInstanceId(ticket.getWfInstanceId());
        vo.setSlaDeadline(ticket.getSlaDeadline());
        vo.setSlaResponseDeadline(ticket.getSlaResponseDeadline());
        vo.setSlaBreached(ticket.getSlaBreached() != null && ticket.getSlaBreached() == 1);
        vo.setCreateTime(ticket.getCreateTime());
        vo.setCreator(buildUserRef(ticket.getCreatorId()));
        vo.setAssignee(buildUserRef(ticket.getAssigneeId()));

        // 评论
        List<TicketComment> comments = commentMapper.selectList(
                Wrappers.<TicketComment>lambdaQuery()
                        .eq(TicketComment::getTicketId, id)
                        .orderByAsc(TicketComment::getId));
        Map<Long, String> commentUsers = loadUserNames(comments.stream()
                .map(TicketComment::getUserId).toList());
        vo.setComments(comments.stream().map(c -> {
            CommentVO cv = new CommentVO();
            cv.setId(c.getId());
            cv.setContent(c.getContent());
            cv.setCreateTime(c.getCreateTime());
            UserRefVO u = new UserRefVO();
            u.setId(c.getUserId());
            u.setNickname(commentUsers.getOrDefault(c.getUserId(), String.valueOf(c.getUserId())));
            cv.setUser(u);
            return cv;
        }).toList());

        // 附件
        List<TicketAttachment> attachments = attachmentMapper.selectList(
                Wrappers.<TicketAttachment>lambdaQuery()
                        .eq(TicketAttachment::getTicketId, id)
                        .orderByDesc(TicketAttachment::getId));
        vo.setAttachments(attachments.stream().map(a -> {
            AttachmentVO av = new AttachmentVO();
            av.setId(a.getId());
            av.setFileName(a.getFileName());
            av.setFileSize(a.getFileSize());
            av.setFilePath(a.getFilePath());
            av.setUploadTime(a.getCreateTime());
            return av;
        }).toList());

        // 操作日志
        List<TicketLog> logs = ticketLogMapper.selectList(
                Wrappers.<TicketLog>lambdaQuery()
                        .eq(TicketLog::getTicketId, id)
                        .orderByDesc(TicketLog::getId));
        Map<Long, String> operatorNames = loadUserNames(logs.stream()
                .map(TicketLog::getOperatorId).toList());
        vo.setLogs(logs.stream().map(l -> {
            LogVO lv = new LogVO();
            lv.setAction(l.getAction());
            lv.setContent(l.getContent());
            lv.setCreateTime(l.getCreateTime());
            lv.setOperatorName(operatorNames.getOrDefault(l.getOperatorId(), String.valueOf(l.getOperatorId())));
            return lv;
        }).toList());
        return vo;
    }

    /**
     * 提交工单（草稿 → 待审批/待指派）
     */
    @Transactional
    public void submit(Long id, Long operatorId, String operatorName) {
        Ticket ticket = getById(id);
        if (!ticket.getCreatorId().equals(operatorId) && !isAdmin(operatorName)) {
            throw new BusinessException(ErrorCode.TICKET_NO_PERMISSION);
        }
        String toStatus = ticket.getTemplateId() != null
                ? TicketStatus.PENDING_APPROVAL.getCode()
                : TicketStatus.PENDING_ASSIGN.getCode();
        transitionValidated(ticket, toStatus, "SUBMIT", "提交工单", operatorId, operatorName);
    }

    /**
     * 指派工单 → 处理中
     */
    @Transactional
    public void assign(Long id, Long assigneeId, Long operatorId, String operatorName) {
        Ticket ticket = getById(id);
        if (!canOperate(ticket, operatorId, operatorName)) {
            throw new BusinessException(ErrorCode.TICKET_NO_PERMISSION);
        }
        ticket.setAssigneeId(assigneeId);
        String toStatus = TicketStatus.IN_PROGRESS.getCode();
        transitionValidated(ticket, toStatus, "ASSIGN", "指派给处理人", operatorId, operatorName);
        // 通知处理人
        notificationService.notify(assigneeId, "工单已指派",
                "您有一个工单被指派处理：" + ticket.getTicketNo() + " " + ticket.getTitle(),
                1, ticket.getId(), "TICKET");
    }

    /**
     * 解决工单 → 已解决
     */
    @Transactional
    public void resolve(Long id, String resolution, Long operatorId, String operatorName) {
        Ticket ticket = getById(id);
        if (!canOperate(ticket, operatorId, operatorName)) {
            throw new BusinessException(ErrorCode.TICKET_NO_PERMISSION);
        }
        transitionValidated(ticket, TicketStatus.RESOLVED.getCode(),
                "RESOLVE", resolution, operatorId, operatorName);
        ticket.setResolvedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    /**
     * 关闭工单 → 已关闭
     */
    @Transactional
    public void close(Long id, Long operatorId, String operatorName) {
        Ticket ticket = getById(id);
        if (!canOperate(ticket, operatorId, operatorName)) {
            throw new BusinessException(ErrorCode.TICKET_NO_PERMISSION);
        }
        transitionValidated(ticket, TicketStatus.CLOSED.getCode(),
                "CLOSE", "关闭工单", operatorId, operatorName);
        ticket.setClosedTime(LocalDateTime.now());
        ticketMapper.updateById(ticket);
    }

    /**
     * 重新打开工单 → 重新打开
     */
    @Transactional
    public void reopen(Long id, String reason, Long operatorId, String operatorName) {
        Ticket ticket = getById(id);
        if (!canOperate(ticket, operatorId, operatorName)) {
            throw new BusinessException(ErrorCode.TICKET_NO_PERMISSION);
        }
        transitionValidated(ticket, TicketStatus.REOPENED.getCode(),
                "REOPEN", reason, operatorId, operatorName);
    }

    /**
     * 看板统计数据
     */
    public DashboardVO dashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setTotal(countByStatus(null));
        vo.setPendingApproval(countByStatus(TicketStatus.PENDING_APPROVAL.getCode()));
        vo.setInProgress(countByStatus(TicketStatus.IN_PROGRESS.getCode()));
        vo.setResolved(countByStatus(TicketStatus.RESOLVED.getCode()));

        Map<String, Long> byType = new LinkedHashMap<>();
        for (int type = 1; type <= 4; type++) {
            byType.put(TicketType.nameOf(type), countByField(Ticket::getTicketType, type, null));
        }
        vo.setByType(byType);

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (int p = 0; p <= 3; p++) {
            byPriority.put(TicketPriority.nameOf(p), countByField(Ticket::getPriority, p, null));
        }
        vo.setByPriority(byPriority);

        // 平均解决时长（小时）与 SLA 达标率（针对已解决工单）
        List<Ticket> resolved = ticketMapper.selectList(
                Wrappers.<Ticket>lambdaQuery()
                        .isNotNull(Ticket::getResolvedTime));
        if (resolved.isEmpty()) {
            vo.setAvgMttrHours(0D);
            vo.setSlaComplianceRate(0D);
            return vo;
        }
        double totalHours = 0;
        long slaOk = 0;
        for (Ticket t : resolved) {
            if (t.getCreateTime() != null && t.getResolvedTime() != null) {
                totalHours += Duration.between(t.getCreateTime(), t.getResolvedTime()).toMillis() / 3600000.0;
            }
            if (t.getSlaDeadline() == null || !t.getResolvedTime().isAfter(t.getSlaDeadline())) {
                slaOk++;
            }
        }
        vo.setAvgMttrHours(Math.round(totalHours / resolved.size() * 100.0) / 100.0);
        vo.setSlaComplianceRate(Math.round(slaOk * 10000.0 / resolved.size()) / 100.0);
        return vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 状态机校验并流转
     */
    private void transitionValidated(Ticket ticket, String toStatus, String action,
                                     String content, Long operatorId, String operatorName) {
        String from = ticket.getStatus();
        if (!isAllowed(from, toStatus)) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_NOT_ALLOWED);
        }
        ticket.setStatus(toStatus);
        ticket.setUpdateBy(operatorName);
        ticketMapper.updateById(ticket);
        recordLog(ticket.getId(), action, from, toStatus, operatorId, content);
        log.info("工单状态流转: id={}, {} -> {}, by={}", ticket.getId(), from, toStatus, operatorId);
    }

    /**
     * 记录工单操作日志
     */
    private void recordLog(Long ticketId, String action, String from, String to,
                           Long operatorId, String content) {
        TicketLog ticketLog = new TicketLog();
        ticketLog.setTicketId(ticketId);
        ticketLog.setAction(action);
        ticketLog.setFromStatus(from);
        ticketLog.setToStatus(to);
        ticketLog.setOperatorId(operatorId);
        ticketLog.setContent(content);
        ticketLogMapper.insert(ticketLog);
    }

    /**
     * 生成工单编号：OPS-{类型}-{yyyyMMdd}-{三位序号}
     */
    private String generateTicketNo(Integer ticketType) {
        String date = LocalDate.now().format(DATE_FMT);
        String prefix = "OPS-" + TicketType.shortCode(ticketType) + "-" + date + "-";
        Long count = ticketMapper.countByNoPrefix(prefix);
        int seq = (count == null ? 0 : count.intValue()) + 1;
        return prefix + String.format("%03d", seq);
    }

    /**
     * 读取 SLA 配置（秒），未配置时返回默认值
     */
    private long slaSeconds(String prefix, Integer priority) {
        String key = prefix + ".p" + (priority == null ? 2 : priority);
        SysConfig config = configMapper.selectOne(
                Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, key));
        if (config != null && StringUtils.hasText(config.getConfigValue())) {
            try {
                return Long.parseLong(config.getConfigValue());
            } catch (NumberFormatException ignored) {
                // 配置解析失败，走默认值
            }
        }
        // 默认值：响应 高1h/中2h/低4h/紧急15min；解决 高8h/中24h/低72h/紧急4h
        return switch (prefix) {
            case "sla.response" -> switch (priority == null ? 2 : priority) {
                case 0 -> 900;
                case 1 -> 3600;
                case 3 -> 14400;
                default -> 7200;
            };
            default -> switch (priority == null ? 2 : priority) {
                case 0 -> 14400;
                case 1 -> 28800;
                case 3 -> 259200;
                default -> 86400;
            };
        };
    }

    private Map<String, Object> parseParams(String scriptParams) {
        if (!StringUtils.hasText(scriptParams)) {
            return null;
        }
        try {
            return objectMapper.readValue(scriptParams, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private Ticket getById(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "工单不存在");
        }
        return ticket;
    }

    private boolean isAdmin(String operatorName) {
        return ADMIN_USERNAME.equals(operatorName);
    }

    private boolean canOperate(Ticket ticket, Long operatorId, String operatorName) {
        return ticket.getCreatorId().equals(operatorId)
                || (ticket.getAssigneeId() != null && ticket.getAssigneeId().equals(operatorId))
                || isAdmin(operatorName);
    }

    private boolean isAllowed(String from, String to) {
        return switch (from) {
            case "DRAFT" -> Set.of("PENDING_APPROVAL", "PENDING_ASSIGN").contains(to);
            case "PENDING_APPROVAL" -> Set.of("APPROVED", "REJECTED", "PENDING_ASSIGN").contains(to);
            case "APPROVED" -> Set.of("PENDING_ASSIGN", "IN_PROGRESS", "EXECUTING").contains(to);
            case "REJECTED" -> Set.of("CLOSED", "DRAFT").contains(to);
            case "PENDING_ASSIGN" -> Set.of("IN_PROGRESS").contains(to);
            case "IN_PROGRESS" -> Set.of("RESOLVED", "PENDING_APPROVAL", "EXECUTING").contains(to);
            case "EXECUTING" -> Set.of("EXEC_SUCCESS", "EXEC_FAILED").contains(to);
            case "EXEC_SUCCESS" -> Set.of("RESOLVED").contains(to);
            case "EXEC_FAILED" -> Set.of("IN_PROGRESS", "CLOSED").contains(to);
            case "RESOLVED" -> Set.of("CLOSED", "REOPENED").contains(to);
            case "REOPENED" -> Set.of("IN_PROGRESS").contains(to);
            case "CLOSED" -> Set.of("REOPENED").contains(to);
            default -> false;
        };
    }

    private TicketVO toVO(Ticket t, Map<Long, String> nameMap) {
        TicketVO vo = new TicketVO();
        vo.setId(t.getId());
        vo.setTicketNo(t.getTicketNo());
        vo.setTitle(t.getTitle());
        vo.setTicketType(t.getTicketType());
        vo.setTicketTypeName(TicketType.nameOf(t.getTicketType()));
        vo.setPriority(t.getPriority());
        vo.setPriorityName(TicketPriority.nameOf(t.getPriority()));
        vo.setStatus(t.getStatus());
        vo.setStatusName(TicketStatus.nameOf(t.getStatus()));
        vo.setCreatorName(nameMap.getOrDefault(t.getCreatorId(), String.valueOf(t.getCreatorId())));
        vo.setAssigneeName(t.getAssigneeId() == null ? null
                : nameMap.getOrDefault(t.getAssigneeId(), String.valueOf(t.getAssigneeId())));
        vo.setSlaBreached(t.getSlaBreached() != null && t.getSlaBreached() == 1);
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    private UserRefVO buildUserRef(Long userId) {
        if (userId == null) {
            return null;
        }
        UserRefVO ref = new UserRefVO();
        ref.setId(userId);
        SysUser user = userMapper.selectById(userId);
        ref.setNickname(user == null ? String.valueOf(userId) : user.getNickname());
        return ref;
    }

    /**
     * 加载工单维度上的用户名称映射
     */
    private Map<Long, String> loadUserNames(List<Ticket> tickets,
                                            Function<Ticket, Long>... idExtractors) {
        List<Long> ids = new ArrayList<>();
        for (Ticket t : tickets) {
            for (Function<Ticket, Long> extractor : idExtractors) {
                Long v = extractor.apply(t);
                if (v != null) {
                    ids.add(v);
                }
            }
        }
        return loadUserNames(ids);
    }

    private Map<Long, String> loadUserNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<Long> distinct = userIds.stream().distinct().toList();
        List<SysUser> users = userMapper.selectBatchIds(distinct);
        return users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getNickname));
    }

    private Long countByStatus(String status) {
        return ticketMapper.selectCount(
                Wrappers.<Ticket>lambdaQuery()
                        .eq(status != null, Ticket::getStatus, status));
    }

    private Long countByField(com.baomidou.mybatisplus.core.toolkit.support.SFunction<Ticket, ?> field,
                              Object value, String status) {
        return ticketMapper.selectCount(
                Wrappers.<Ticket>lambdaQuery()
                        .eq(field, value)
                        .eq(status != null, Ticket::getStatus, status));
    }
}