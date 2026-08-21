package com.opsflow.module.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.opsflow.module.knowledge.mapper.KbArticleMapper;
import com.opsflow.module.knowledge.model.dto.KbQaDTO;
import com.opsflow.module.knowledge.model.entity.KbArticle;
import com.opsflow.module.knowledge.model.vo.KbQaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 智能问答服务（RAG 简化实现）
 * 基于关键词对已发布文章内容做相关性检索，取 Top-K 相关片段作为引用来源，
 * 并基于片段摘要生成答案。生产环境可替换为 Embedding + pgvector + LLM。
 */
@Service
@RequiredArgsConstructor
public class KbQaService {

    private final KbArticleMapper articleMapper;

    /** 返回片段数量 */
    private static final int TOP_K = 5;

    public KbQaVO ask(KbQaDTO dto) {
        String question = dto.getQuestion();
        String conversationId = StringUtils.hasText(dto.getConversationId())
                ? dto.getConversationId() : "conv-" + UUID.randomUUID();

        // 已发布文章
        List<KbArticle> articles = articleMapper.selectList(
                Wrappers.<KbArticle>lambdaQuery().eq(KbArticle::getStatus, 1));
        if (articles.isEmpty()) {
            KbQaVO vo = new KbQaVO();
            vo.setAnswer("知识库暂无已发布文章，无法回答该问题。");
            vo.setSources(List.of());
            vo.setConversationId(conversationId);
            return vo;
        }

        // 关键词切分（简单按空白/常见标点）
        List<String> keywords = extractKeywords(question);

        // 计算每篇文章相关性得分
        List<ScoredArticle> scored = new ArrayList<>();
        for (KbArticle article : articles) {
            String text = (article.getTitle() + " " + article.getContent()).toLowerCase();
            double score = 0;
            for (String kw : keywords) {
                if (text.contains(kw)) {
                    score += 1;
                }
            }
            if (score > 0) {
                ScoredArticle sa = new ScoredArticle();
                sa.article = article;
                sa.score = score;
                scored.add(sa);
            }
        }

        // 按得分降序取 Top-K
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<ScoredArticle> top = scored.size() > TOP_K ? scored.subList(0, TOP_K) : scored;

        // 构建来源与答案
        KbQaVO vo = new KbQaVO();
        List<KbQaVO.SourceVO> sources = new ArrayList<>();
        StringBuilder answer = new StringBuilder();
        if (top.isEmpty()) {
            answer.append("未在知识库中找到与问题直接相关的内容。可尝试：\n")
                    .append("1. 换用更精确的关键词\n")
                    .append("2. 在「文章管理」中新增相关文章\n")
                    .append("3. 将已关闭工单归档为知识");
        } else {
            answer.append("根据知识库检索到以下相关内容供参考：\n\n");
            int idx = 1;
            for (ScoredArticle sa : top) {
                KbQaVO.SourceVO src = new KbQaVO.SourceVO();
                src.setArticleId(sa.article.getId());
                src.setTitle(sa.article.getTitle());
                src.setRelevance(sa.score);
                sources.add(src);
                answer.append(idx).append(". 「").append(sa.article.getTitle()).append("」\n");
                answer.append("   ").append(extractSnippet(sa.article.getContent(), question)).append("\n\n");
                idx++;
            }
            answer.append("以上内容的完整流程请点击对应引用来源查看文章。");
        }
        vo.setAnswer(answer.toString());
        vo.setSources(sources);
        vo.setConversationId(conversationId);
        return vo;
    }

    /** 提取关键词（按空白和标点切分，过滤停用词） */
    private List<String> extractKeywords(String question) {
        List<String> stops = List.of("的", "了", "怎么", "如何", "什么", "？", "?", "，", "。", "再");
        String[] parts = question.toLowerCase().split("[\\s\\p{Punct}，。？、!！；;：:]");
        List<String> keywords = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.length() >= 2 && !stops.contains(t)) {
                keywords.add(t);
            }
        }
        return keywords;
    }

    /** 提取内容片段（截取含关键词的一段） */
    private String extractSnippet(String content, String question) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String plain = content.replaceAll("[#*`\\[\\]()>\\-]", " ").replaceAll("\\s+", " ").trim();
        String lower = plain.toLowerCase();
        for (String kw : extractKeywords(question)) {
            int idx = lower.indexOf(kw);
            if (idx >= 0) {
                int start = Math.max(0, idx - 40);
                int end = Math.min(plain.length(), idx + 60);
                return "..." + plain.substring(start, end) + "...";
            }
        }
        return plain.length() > 100 ? plain.substring(0, 100) + "..." : plain;
    }

    /** 带得分的文章 */
    private static class ScoredArticle {
        KbArticle article;
        double score;
    }
}