package com.opsflow.module.ticket.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单主表实体（ticket 表）
 */
@Data
@TableName("ticket")
public class Ticket implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号（OPS-CHG-20260816-001） */
    private String ticketNo;

    /** 工单标题 */
    private String title;

    /** 工单描述（支持 Markdown） */
    private String description;

    /** 工单类型：1=变更 2=故障 3=请求 4=巡检 */
    private Integer ticketType;

    /** 优先级：0=紧急 1=高 2=中 3=低 */
    private Integer priority;

    /** 当前状态 */
    private String status;

    /** 创建人 ID */
    private Long creatorId;

    /** 当前处理人 ID */
    private Long assigneeId;

    /** 关联的表单模板 ID */
    private Long templateId;

    /** 关联的流程实例 ID */
    private Long wfInstanceId;

    /** 关联的目标主机 ID */
    private Long hostId;

    /** 关联的自动化脚本 ID */
    private Long scriptId;

    /** 脚本执行参数（JSON） */
    private String scriptParams;

    /** SLA 解决截止时间 */
    private LocalDateTime slaDeadline;

    /** SLA 响应截止时间 */
    private LocalDateTime slaResponseDeadline;

    /** 是否已超 SLA：0=否 1=是 */
    private Integer slaBreached;

    /** 解决时间 */
    private LocalDateTime resolvedTime;

    /** 关闭时间 */
    private LocalDateTime closedTime;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 备注 */
    private String remark;
}