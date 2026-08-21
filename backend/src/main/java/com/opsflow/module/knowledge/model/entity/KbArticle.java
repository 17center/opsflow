package com.opsflow.module.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文章实体（kb_article 表）
 */
@Data
@TableName("kb_article")
public class KbArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章内容（Markdown） */
    private String content;

    /** 分类：1=故障排查 2=操作手册 3=最佳实践 4=FAQ */
    private Integer category;

    /** 状态：0=草稿 1=已发布 2=审核中 */
    private Integer status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 关联工单 ID（从工单转知识时有值） */
    private Long relatedTicketId;

    /** 作者 ID */
    private Long authorId;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}