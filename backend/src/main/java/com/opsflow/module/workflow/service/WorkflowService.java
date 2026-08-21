package com.opsflow.module.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.auth.mapper.SysRoleMapper;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.mapper.SysUserRoleMapper;
import com.opsflow.module.auth.model.entity.SysRole;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.auth.model.entity.SysUserRole;
import com.opsflow.module.system.service.NotificationService;
import com.opsflow.module.ticket.mapper.TicketLogMapper;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.entity.TicketLog;
import com.opsflow.module.workflow.mapper.WfDefinitionMapper;
import com.opsflow.module.workflow.mapper.WfInstanceMapper;
import com.opsflow.module.workflow.mapper.WfTaskMapper;
import com.opsflow.module.workflow.model.dto.WfInstanceStartDTO;
import com.opsflow.module.workflow.model.dto.WfNodeDTO;
import com.opsflow.module.workflow.model.dto.WfTaskCommentDTO;
import com.opsflow.module.workflow.model.dto.WfTaskDelegateDTO;
import com.opsflow.module.workflow.model.entity.WfDefinition;
import com.opsflow.module.workflow.model.entity.WfInstance;
import com.opsflow.module.workflow.model.entity.WfTask;
import com.opsflow.module.workflow.model.vo.WfInstanceVO;
import com.opsflow.module.workflow.model.vo.WfTaskTodoVO;
import com.opsflow.module.workflow.model.vo.WfTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎：启动流程实例、审批（通过/驳回/转交）、待办、实例详情
 * 采用线性人工审批节点模型，节点配置以 JSON 存储于 wf_definition.bpmn_xml。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String ADMIN_USERNAME = "admin";

    // 任务状态
    private static final int TASK_PENDING = 1;
    private static final int TASK_APPROVED = 2;
    private static final int TASK_REJECTED = 3;
    private static final int TASK_DELEGATED = 4;

    // 实例状态
    private static final int INST_RUNNING = 1;
    private static final int INST_COMPLETED = 2;
    private static final int INST_TERMINATED = 3;

    // 工单状态
    private static final String TICKET_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String TICKET_APPROVED = "APPROVED";
    private static final String TICKET_REJECTED = "REJECTED";

    private final WfDefinitionMapper definitionMapper;
    private final WfInstanceMapper instanceMapper;
    private final WfTaskMapper taskMapper;
    private final WfDefinitionService wfDefinitionService;
    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final NotificationService notificationService;

    /**
     * 启动流程实例：创建实例、置工单待审批、创建首个审批任务
     */
    @Transactional
    public Long start(Long ticketId, Long definitionId, Long operatorId, String operatorName) {
        Ticket ticket = getTicket(ticketId);
        WfDefinition definition = getDefinition(definitionId);
        if (!Objects.equals(definition.getStatus(), 1)) {
            throw new BusinessException(ErrorCode.WF_NOT_FOUND.getCode(), "流程未发布");
        }
        // 同一工单仅允许一个运行中的流程
        Long running = instanceMapper.selectCount(
                Wrappers.<WfInstance>lambdaQuery()
                        .eq(WfInstance::getTicketId, ticketId)
                        .eq(WfInstance::getStatus, INST_RUNNING));
        if (running != null && running > 0) {
            throw new BusinessException(ErrorCode.WF_TICKET_ALREADY_PROCESS);
        }
        List<WfNodeDTO> approvalNodes = wfDefinitionService.parseNodes(definition.getBpmnXml()).stream()
                .filter(n -> Objects.equals(n.getNodeType(), 1))
                .toList();
        if (approvalNodes.isEmpty()) {
            throw new BusinessException(ErrorCode.WF_NO_APPROVAL_NODE);
        }

        WfInstance instance = new WfInstance();
        instance.setWfDefId(definitionId);
        instance.setTicketId(ticketId);
        instance.setStatus(INST_RUNNING);
        instance.setStartTime(LocalDateTime.now());
        instance.setCreateBy(operatorName);
        instanceMapper.insert(instance);

        // 更新工单状态 → 待审批，并关联流程实例
        ticket.setStatus(TICKET_PENDING_APPROVAL);
        ticket.setWfInstanceId(instance.getId());
        ticket.setUpdateBy(operatorName);
        ticketMapper.updateById(ticket);
        recordTicketLog(ticketId, "SUBMIT", "启动审批流程：" + definition.getName(), operatorId);

        // 创建首个审批任务
        createTask(instance.getId(), approvalNodes.get(0));
        log.info("启动流程实例: instanceId={}, ticketId={}, defId={}", instance.getId(), ticketId, definitionId);
        return instance.getId();
    }

    /**
     * 审批通过：完成当前任务，推进到下一审批节点或完成流程
     */
    @Transactional
    public void approve(Long taskId, WfTaskCommentDTO dto, Long operatorId, String operatorName) {
        WfTask task = getTask(taskId);
        WfInstance instance = getRunningInstance(task.getWfInstanceId());
        checkTaskPermission(task, operatorId, operatorName);

        completeTask(task, TASK_APPROVED, "APPROVE", dto.getComment(), operatorName);
        recordTicketLog(instance.getTicketId(), "APPROVE", operatorName + " 通过审批：" + task.getNodeName(), operatorId);

        // 推进到下一审批节点
        WfTask next = createNextTask(instance, task);
        if (next == null) {
            // 流程完成
            finishInstance(instance, INST_COMPLETED);
            Ticket ticket = getTicket(instance.getTicketId());
            ticket.setStatus(TICKET_APPROVED);
            ticket.setUpdateBy(operatorName);
            ticketMapper.updateById(ticket);
            recordTicketLog(instance.getTicketId(), "APPROVE", "审批全部通过，工单进入执行阶段", operatorId);
        } else {
            notifyAssignee(next);
        }
    }

    /**
     * 审批驳回：终止流程，工单置为已驳回
     */
    @Transactional
    public void reject(Long taskId, WfTaskCommentDTO dto, Long operatorId, String operatorName) {
        WfTask task = getTask(taskId);
        WfInstance instance = getRunningInstance(task.getWfInstanceId());
        checkTaskPermission(task, operatorId, operatorName);

        completeTask(task, TASK_REJECTED, "REJECT", dto.getComment(), operatorName);
        finishInstance(instance, INST_TERMINATED);

        Ticket ticket = getTicket(instance.getTicketId());
        ticket.setStatus(TICKET_REJECTED);
        ticket.setUpdateBy(operatorName);
        ticketMapper.updateById(ticket);
        recordTicketLog(instance.getTicketId(), "REJECT", operatorName + " 驳回：" + dto.getComment(), operatorId);
    }

    /**
     * 转交审批：原任务标记转交，为目标审批人创建新任务
     */
    @Transactional
    public void delegate(Long taskId, WfTaskDelegateDTO dto, Long operatorId, String operatorName) {
        WfTask task = getTask(taskId);
        getRunningInstance(task.getWfInstanceId());
        checkTaskPermission(task, operatorId, operatorName);

        completeTask(task, TASK_DELEGATED, "DELEGATE", dto.getComment(), operatorName);

        WfTask newTask = new WfTask();
        newTask.setWfInstanceId(task.getWfInstanceId());
        newTask.setNodeKey(task.getNodeKey());
        newTask.setNodeName(task.getNodeName());
        newTask.setNodeType(task.getNodeType());
        newTask.setAssigneeId(dto.getTargetUserId());
        newTask.setSignType(task.getSignType());
        newTask.setStatus(TASK_PENDING);
        newTask.setTimeoutHours(task.getTimeoutHours());
        newTask.setDueTime(LocalDateTime.now().plusHours(task.getTimeoutHours() == null ? 24 : task.getTimeoutHours()));
        newTask.setCreateBy(operatorName);
        taskMapper.insert(newTask);
        notifyAssignee(newTask);
    }

    /**
     * 我的待办任务
     */
    public PageResult<WfTaskTodoVO> todo(Long userId, long current, long size) {
        Set<String> roleCodes = loadUserRoleCodes(userId);
        Page<WfTask> page = taskMapper.selectPage(new Page<>(current, size),
                Wrappers.<WfTask>lambdaQuery()
                        .eq(WfTask::getStatus, TASK_PENDING)
                        .and(w -> {
                            w.eq(WfTask::getAssigneeId, userId);
                            if (roleCodes != null && !roleCodes.isEmpty()) {
                                w.or().in(WfTask::getCandidateGroup, roleCodes);
                            }
                        })
                        .orderByAsc(WfTask::getDueTime));
        List<WfTaskTodoVO> records = page.getRecords().stream()
                .map(t -> toTodoVO(t, userId))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 流程实例列表（分页）
     */
    public PageResult<WfInstanceVO> pageInstances(long current, long size, Long ticketId, Integer status) {
        Page<WfInstance> page = instanceMapper.selectPage(new Page<>(current, size),
                Wrappers.<WfInstance>lambdaQuery()
                        .eq(ticketId != null, WfInstance::getTicketId, ticketId)
                        .eq(status != null, WfInstance::getStatus, status)
                        .orderByDesc(WfInstance::getId));
        List<WfInstanceVO> records = page.getRecords().stream()
                .map(i -> instanceDetail(i.getId()))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    /**
     * 流程实例详情（含任务轨迹）
     */
    public WfInstanceVO instanceDetail(Long instanceId) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "流程实例不存在");
        }
        WfInstanceVO vo = new WfInstanceVO();
        vo.setId(instance.getId());
        vo.setWfDefId(instance.getWfDefId());
        vo.setTicketId(instance.getTicketId());
        vo.setStatus(instance.getStatus());
        vo.setStatusName(instanceStatusName(instance.getStatus()));
        vo.setStartTime(instance.getStartTime());
        vo.setEndTime(instance.getEndTime());

        WfDefinition definition = definitionMapper.selectById(instance.getWfDefId());
        if (definition != null) {
            vo.setWfDefName(definition.getName());
            vo.setWfDefVersion(definition.getVersion());
        }
        Ticket ticket = ticketMapper.selectById(instance.getTicketId());
        if (ticket != null) {
            vo.setTicketNo(ticket.getTicketNo());
            vo.setTicketTitle(ticket.getTitle());
        }

        List<WfTask> tasks = taskMapper.selectList(
                Wrappers.<WfTask>lambdaQuery()
                        .eq(WfTask::getWfInstanceId, instanceId)
                        .orderByAsc(WfTask::getId));
        Map<Long, String> nameMap = loadUserNames(tasks.stream()
                .map(WfTask::getAssigneeId).toList());
        vo.setTasks(tasks.stream().map(t -> toTaskVO(t, nameMap)).toList());
        return vo;
    }

    // ==================== 私有方法 ====================

    private WfTask createTask(Long instanceId, WfNodeDTO node) {
        WfTask task = new WfTask();
        task.setWfInstanceId(instanceId);
        task.setNodeKey(node.getNodeKey());
        task.setNodeName(node.getNodeName());
        task.setNodeType(node.getNodeType() == null ? 1 : node.getNodeType());
        task.setAssigneeId(node.getAssigneeId());
        task.setCandidateGroup(node.getCandidateGroup());
        task.setSignType(node.getSignType() == null ? 1 : node.getSignType());
        task.setStatus(TASK_PENDING);
        task.setTimeoutHours(24);
        task.setDueTime(LocalDateTime.now().plusHours(24));
        task.setSignedCount(0);
        task.setRequiredCount(1);
        task.setCreateBy("workflow");
        taskMapper.insert(task);
        return task;
    }

    /**
     * 创建当前节点的下一审批节点任务；无下一节点返回 null
     */
    private WfTask createNextTask(WfInstance instance, WfTask current) {
        WfDefinition definition = getDefinition(instance.getWfDefId());
        List<WfNodeDTO> approvalNodes = wfDefinitionService.parseNodes(definition.getBpmnXml()).stream()
                .filter(n -> Objects.equals(n.getNodeType(), 1))
                .toList();
        int idx = -1;
        for (int i = 0; i < approvalNodes.size(); i++) {
            if (Objects.equals(approvalNodes.get(i).getNodeKey(), current.getNodeKey())) {
                idx = i;
                break;
            }
        }
        if (idx < 0 || idx + 1 >= approvalNodes.size()) {
            return null;
        }
        return createTask(instance.getId(), approvalNodes.get(idx + 1));
    }

    private void completeTask(WfTask task, int status, String action, String comment, String operatorName) {
        task.setStatus(status);
        task.setAction(action);
        task.setComment(comment);
        task.setCompleteTime(LocalDateTime.now());
        task.setUpdateBy(operatorName);
        taskMapper.updateById(task);
    }

    private void finishInstance(WfInstance instance, int status) {
        instance.setStatus(status);
        instance.setEndTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
    }

    private void notifyAssignee(WfTask task) {
        if (task.getAssigneeId() != null) {
            notificationService.notify(task.getAssigneeId(), "您有新的审批任务",
                    "请审批：" + task.getNodeName(), 2, task.getId(), "TASK");
        }
    }

    private void checkTaskPermission(WfTask task, Long operatorId, String operatorName) {
        if (ADMIN_USERNAME.equals(operatorName)) {
            return;
        }
        boolean assigned = task.getAssigneeId() != null && task.getAssigneeId().equals(operatorId);
        boolean byGroup = StringUtils.hasText(task.getCandidateGroup())
                && loadUserRoleCodes(operatorId).contains(task.getCandidateGroup());
        if (!assigned && !byGroup) {
            throw new BusinessException(ErrorCode.WF_TASK_NO_PERMISSION);
        }
    }

    private WfTaskTodoVO toTodoVO(WfTask task, Long _userId) {
        WfTaskTodoVO vo = new WfTaskTodoVO();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getNodeName());
        vo.setWfInstanceId(task.getWfInstanceId());
        vo.setSignType(task.getSignType());
        vo.setDueTime(task.getDueTime());
        vo.setCreateTime(task.getCreateTime());
        WfInstance instance = instanceMapper.selectById(task.getWfInstanceId());
        if (instance != null) {
            vo.setTicketId(instance.getTicketId());
            Ticket ticket = ticketMapper.selectById(instance.getTicketId());
            if (ticket != null) {
                vo.setTicketNo(ticket.getTicketNo());
                vo.setTicketTitle(ticket.getTitle());
            }
        }
        return vo;
    }

    private WfTaskVO toTaskVO(WfTask task, Map<Long, String> nameMap) {
        WfTaskVO vo = new WfTaskVO();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getNodeName());
        vo.setNodeKey(task.getNodeKey());
        vo.setAssigneeName(task.getAssigneeId() == null ? null
                : nameMap.getOrDefault(task.getAssigneeId(), String.valueOf(task.getAssigneeId())));
        vo.setStatus(task.getStatus());
        vo.setStatusName(taskStatusName(task.getStatus()));
        vo.setAction(task.getAction());
        vo.setComment(task.getComment());
        vo.setDueTime(task.getDueTime());
        vo.setCompleteTime(task.getCompleteTime());
        return vo;
    }

    private Set<String> loadUserRoleCodes(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<SysUserRole> links = userRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = links.stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toSet());
    }

    private Map<Long, String> loadUserNames(List<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(distinct);
            for (SysUser user : users) {
                map.put(user.getId(), user.getNickname());
            }
        }
        return map;
    }

    private void recordTicketLog(Long ticketId, String action, String content, Long operatorId) {
        TicketLog ticketLog = new TicketLog();
        ticketLog.setTicketId(ticketId);
        ticketLog.setAction(action);
        ticketLog.setOperatorId(operatorId);
        ticketLog.setContent(content);
        ticketLogMapper.insert(ticketLog);
    }

    private WfTask getTask(Long taskId) {
        WfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.WF_TASK_NOT_FOUND);
        }
        if (!Objects.equals(task.getStatus(), TASK_PENDING)) {
            throw new BusinessException(ErrorCode.WF_TASK_DONE);
        }
        return task;
    }

    private WfInstance getRunningInstance(Long instanceId) {
        WfInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null || !Objects.equals(instance.getStatus(), INST_RUNNING)) {
            throw new BusinessException(ErrorCode.WF_INSTANCE_NOT_RUNNING);
        }
        return instance;
    }

    private Ticket getTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "工单不存在");
        }
        return ticket;
    }

    private WfDefinition getDefinition(Long definitionId) {
        WfDefinition definition = definitionMapper.selectById(definitionId);
        if (definition == null) {
            throw new BusinessException(ErrorCode.WF_NOT_FOUND);
        }
        return definition;
    }

    private String taskStatusName(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 1 -> "待处理";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            case 4 -> "已转交";
            case 5 -> "已超时";
            default -> "-";
        };
    }

    private String instanceStatusName(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 1 -> "运行中";
            case 2 -> "已完成";
            case 3 -> "已终止";
            default -> "-";
        };
    }
}