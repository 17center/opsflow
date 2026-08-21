package com.opsflow.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签 VO
 */
@Data
public class KbTagVO {

    private Long id;

    private String name;

    private LocalDateTime createTime;
}