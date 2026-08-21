package com.opsflow.module.ticket.model.vo;

import lombok.Data;

/**
 * 用户简要信息（工单中嵌套展示）
 */
@Data
public class UserRefVO {

    private Long id;

    private String nickname;
}