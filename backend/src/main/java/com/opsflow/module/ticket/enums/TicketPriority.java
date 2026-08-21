package com.opsflow.module.ticket.enums;

import lombok.Getter;

/**
 * 工单优先级
 */
@Getter
public enum TicketPriority {

    URGENT(0, "紧急"),
    HIGH(1, "高"),
    MEDIUM(2, "中"),
    LOW(3, "低");

    private final Integer code;
    private final String name;

    TicketPriority(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码取名称，未知编码返回 null
     */
    public static String nameOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (TicketPriority priority : values()) {
            if (priority.code.equals(code)) {
                return priority.name;
            }
        }
        return null;
    }
}