package com.opsflow.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文章 VO
 */
@Data
public class KbArticleVO {

    private Long id;

    private String title;

    private String content;

    /** 分类：1=故障排查 2=操作手册 3=最佳实践 4=FAQ */
    private Integer category;

    private String categoryName;

    /** 状态：0=草稿 1=已发布 2=审核中 */
    private Integer status;

    private String statusName;

    /** 浏览次数 */
    private Integer viewCount;

    /** 关联工单 ID */
    private Long relatedTicketId;

    /** 作者 ID */
    private Long authorId;

    private String authorName;

    /** 标签列表 */
    private List<Long> tagIds;

    private List<String> tagNames;

    private LocalDateTime createTime;

    private String remark;
}