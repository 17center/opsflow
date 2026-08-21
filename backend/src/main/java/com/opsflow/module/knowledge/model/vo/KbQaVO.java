package com.opsflow.module.knowledge.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 智能问答 VO
 */
@Data
public class KbQaVO {

    /** 生成的答案 */
    private String answer;

    /** 引用来源 */
    private List<SourceVO> sources;

    /** 会话 ID */
    private String conversationId;

    /** 引用来源 */
    @Data
    public static class SourceVO {
        private Long articleId;
        private String title;
        private Double relevance;
    }
}