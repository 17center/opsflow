package com.opsflow.module.ticket.enums;

import lombok.Getter;

/**
 * 工单状态（与数据库 ticket.status 对应）
 */
@Getter
public enum TicketStatus {

    DRAFT("DRAFT", "草稿"),
    PENDING_APPROVAL("PENDING_APPROVAL", "待审批"),
    APPROVED("APPROVED", "审批通过"),
    REJECTED("REJECTED", "已驳回"),
    PENDING_ASSIGN("PENDING_ASSIGN", "待指派"),
    IN_PROGRESS("IN_PROGRESS", "处理中"),
    EXECUTING("EXECUTING", "执行中"),
    EXEC_SUCCESS("EXEC_SUCCESS", "执行成功"),
    EXEC_FAILED("EXEC_FAILED", "执行失败"),
    RESOLVED("RESOLVED", "已解决"),
    REOPENED("REOPENED", "重新打开"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String name;

    TicketStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码取名称，未知编码返回原值
     */
    public static String nameOf(String code) {
        if (code == null) {
            return null;
        }
        for (TicketStatus status : values()) {
            if (status.code.equals(code)) {
                return status.name;
            }
        }
        return code;
    }
}