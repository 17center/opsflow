package com.opsflow.module.ticket.enums;

import lombok.Getter;

/**
 * 工单类型
 */
@Getter
public enum TicketType {

    CHANGE(1, "变更"),
    FAULT(2, "故障"),
    REQUEST(3, "请求"),
    PATROL(4, "巡检");

    private final Integer code;
    private final String name;

    TicketType(Integer code, String name) {
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
        for (TicketType type : values()) {
            if (type.code.equals(code)) {
                return type.name;
            }
        }
        return null;
    }

    /**
     * 工单编号中的类型缩写（变更=CHG 故障=FLT 请求=REQ 巡检=PAT）
     */
    public static String shortCode(Integer code) {
        if (code == null) {
            return "TKT";
        }
        return switch (code) {
            case 1 -> "CHG";
            case 2 -> "FLT";
            case 3 -> "REQ";
            case 4 -> "PAT";
            default -> "TKT";
        };
    }
}