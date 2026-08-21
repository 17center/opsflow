package com.opsflow.module.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签实体（kb_tag 表）
 */
@Data
@TableName("kb_tag")
public class KbTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称（唯一） */
    private String name;

    private LocalDateTime createTime;
}