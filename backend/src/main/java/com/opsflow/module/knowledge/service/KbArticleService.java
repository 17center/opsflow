package com.opsflow.module.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.opsflow.common.enums.ErrorCode;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.result.PageResult;
import com.opsflow.module.auth.mapper.SysUserMapper;
import com.opsflow.module.auth.model.entity.SysUser;
import com.opsflow.module.knowledge.mapper.KbArticleMapper;
import com.opsflow.module.knowledge.mapper.KbArticleTagMapper;
import com.opsflow.module.knowledge.mapper.KbTagMapper;
import com.opsflow.module.knowledge.model.dto.KbArticleDTO;
import com.opsflow.module.knowledge.model.entity.KbArticle;
import com.opsflow.module.knowledge.model.entity.KbArticleTag;
import com.opsflow.module.knowledge.model.entity.KbTag;
import com.opsflow.module.knowledge.model.vo.KbArticleVO;
import com.opsflow.module.ticket.enums.TicketStatus;
import com.opsflow.module.ticket.mapper.TicketLogMapper;
import com.opsflow.module.ticket.mapper.TicketMapper;
import com.opsflow.module.ticket.model.entity.Ticket;
import com.opsflow.module.ticket.model.entity.TicketLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识文章服务：CRUD、发布、全文搜索、标签关联、工单转知识
 */
@Service
@RequiredArgsConstructor
public class KbArticleService {

    private static final String[] CATEGORY_NAMES = {"", "故障排查", "操作手册", "最佳实践", "FAQ"};
    private static final String[] STATUS_NAMES = {"草稿", "已发布", "审核中"};

    private final KbArticleMapper articleMapper;
    private final KbTagMapper tagMapper;
    private final KbArticleTagMapper articleTagMapper;
    private final SysUserMapper userMapper;
    private final TicketMapper ticketMapper;
    private final TicketLogMapper ticketLogMapper;

    public PageResult<KbArticleVO> page(long current, long size, Integer category, Integer status, String keyword, Long tagId) {
        Page<KbArticle> page = articleMapper.selectPage(
                new Page<>(current, size),
                Wrappers.<KbArticle>lambdaQuery()
                        .eq(category != null, KbArticle::getCategory, category)
                        .eq(status != null, KbArticle::getStatus, status)
                        .and(StringUtils.hasText(keyword), w -> w.like(KbArticle::getTitle, keyword)
                                .or().like(KbArticle::getContent, keyword))
                        .orderByDesc(KbArticle::getCreateTime));
        List<KbArticleVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        // 按标签过滤（在内存中过滤，因标签为多对多关联）
        if (tagId != null) {
            records = records.stream().filter(a -> a.getTagIds() != null && a.getTagIds().contains(tagId))
                    .collect(Collectors.toList());
        }
        return PageResult.of(records, page.getTotal(), page.getSize(), page.getCurrent());
    }

    public KbArticleVO detail(Long id) {
        KbArticle article = requireArticle(id);
        // 浏览量 +1
        articleMapper.update(null, Wrappers.<KbArticle>lambdaUpdate()
                .eq(KbArticle::getId, id)
                .setSql("view_count = view_count + 1"));
        article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        return toVO(article);
    }

    @Transactional
    public void create(KbArticleDTO dto, Long authorId, String operator) {
        KbArticle article = new KbArticle();
        applyDto(article, dto);
        article.setAuthorId(authorId);
        article.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        article.setViewCount(0);
        article.setCreateBy(operator);
        articleMapper.insert(article);
        saveTags(article.getId(), dto.getTagIds());
    }

    @Transactional
    public void update(Long id, KbArticleDTO dto, String operator) {
        KbArticle article = requireArticle(id);
        applyDto(article, dto);
        article.setUpdateBy(operator);
        articleMapper.updateById(article);
        // 重建标签关联
        articleTagMapper.delete(Wrappers.<KbArticleTag>lambdaQuery().eq(KbArticleTag::getArticleId, id));
        saveTags(id, dto.getTagIds());
    }

    @Transactional
    public void delete(Long id) {
        requireArticle(id);
        articleMapper.deleteById(id);
        articleTagMapper.delete(Wrappers.<KbArticleTag>lambdaQuery().eq(KbArticleTag::getArticleId, id));
    }

    @Transactional
    public void publish(Long id, Integer status, String operator) {
        KbArticle article = requireArticle(id);
        article.setStatus(status);
        article.setUpdateBy(operator);
        articleMapper.updateById(article);
    }

    /**
     * 已关闭工单转知识文章草稿
     * 自动提取工单标题、描述、处理过程（操作日志）生成文章
     */
    @Transactional
    public KbArticleVO fromTicket(Long ticketId, Long authorId, String operator) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.TICKET_STATUS_NOT_ALLOWED);
        }
        if (ticket.getStatus() == null || !ticket.getStatus().equals(TicketStatus.CLOSED.getCode())) {
            throw new BusinessException(ErrorCode.KB_TICKET_INVALID);
        }
        // 提取处理过程日志
        List<TicketLog> logs = ticketLogMapper.selectList(
                Wrappers.<TicketLog>lambdaQuery()
                        .eq(TicketLog::getTicketId, ticketId)
                        .orderByAsc(TicketLog::getCreateTime));

        StringBuilder content = new StringBuilder();
        content.append("## 工单信息\n\n");
        content.append("- 工单编号：").append(ticket.getTicketNo()).append("\n");
        content.append("- 工单标题：").append(ticket.getTitle()).append("\n");
        content.append("- 工单描述：\n\n").append(ticket.getDescription() == null ? "" : ticket.getDescription()).append("\n\n");
        content.append("## 处理过程\n\n");
        for (TicketLog log : logs) {
            content.append("- [").append(log.getCreateTime()).append("] ")
                    .append(log.getAction()).append("：")
                    .append(log.getContent() == null ? "" : log.getContent()).append("\n");
        }

        KbArticle article = new KbArticle();
        article.setTitle(ticket.getTitle());
        article.setContent(content.toString());
        article.setCategory(1); // 默认故障排查分类
        article.setStatus(0); // 草稿
        article.setViewCount(0);
        article.setRelatedTicketId(ticketId);
        article.setAuthorId(authorId);
        article.setCreateBy(operator);
        articleMapper.insert(article);
        return toVO(article);
    }

    private void applyDto(KbArticle article, KbArticleDTO dto) {
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setCategory(dto.getCategory());
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
        }
        article.setRemark(dto.getRemark());
    }

    private void saveTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) == null) {
                throw new BusinessException(ErrorCode.KB_TAG_NOT_FOUND);
            }
            KbArticleTag at = new KbArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
    }

    private KbArticle requireArticle(Long id) {
        KbArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.KB_ARTICLE_NOT_FOUND);
        }
        return article;
    }

    private KbArticleVO toVO(KbArticle article) {
        KbArticleVO vo = new KbArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setCategory(article.getCategory());
        vo.setCategoryName(article.getCategory() != null && article.getCategory() < CATEGORY_NAMES.length
                ? CATEGORY_NAMES[article.getCategory()] : String.valueOf(article.getCategory()));
        vo.setStatus(article.getStatus());
        vo.setStatusName(article.getStatus() != null && article.getStatus() < STATUS_NAMES.length
                ? STATUS_NAMES[article.getStatus()] : String.valueOf(article.getStatus()));
        vo.setViewCount(article.getViewCount());
        vo.setRelatedTicketId(article.getRelatedTicketId());
        vo.setAuthorId(article.getAuthorId());
        if (article.getAuthorId() != null) {
            SysUser author = userMapper.selectById(article.getAuthorId());
            if (author != null) {
                vo.setAuthorName(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
            }
        }
        // 标签
        List<KbArticleTag> ats = articleTagMapper.selectList(
                Wrappers.<KbArticleTag>lambdaQuery().eq(KbArticleTag::getArticleId, article.getId()));
        List<Long> tagIds = ats.stream().map(KbArticleTag::getTagId).collect(Collectors.toList());
        vo.setTagIds(tagIds);
        if (!tagIds.isEmpty()) {
            Map<Long, String> tagNameMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(KbTag::getId, KbTag::getName));
            vo.setTagNames(tagIds.stream().map(tagNameMap::get).collect(Collectors.toList()));
        }
        vo.setCreateTime(article.getCreateTime());
        vo.setRemark(article.getRemark());
        return vo;
    }
}