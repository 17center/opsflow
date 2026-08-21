package com.opsflow.module.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章-标签关联实体（kb_article_tag 表）
 */
@Data
@TableName("kb_article_tag")
public class KbArticleTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章 ID */
    private Long articleId;

    /** 标签 ID */
    private Long tagId;

    private LocalDateTime createTime;
}