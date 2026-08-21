package com.opsflow.module.knowledge.controller;

import com.opsflow.common.result.PageResult;
import com.opsflow.common.result.R;
import com.opsflow.module.auth.security.SecurityUtils;
import com.opsflow.module.knowledge.model.dto.KbArticleDTO;
import com.opsflow.module.knowledge.model.vo.KbArticleVO;
import com.opsflow.module.knowledge.service.KbArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 知识文章接口
 */
@Tag(name = "知识文章")
@RestController
@RequestMapping("/api/kb/articles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('kb:article:list')")
public class KbArticleController {

    private final KbArticleService articleService;

    @Operation(summary = "分页查询文章")
    @GetMapping
    public R<PageResult<KbArticleVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long tagId) {
        if (size > 100) {
            size = 100;
        }
        return R.ok(articleService.page(current, size, category, status, keyword, tagId));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/{id}")
    public R<KbArticleVO> detail(@PathVariable Long id) {
        return R.ok(articleService.detail(id));
    }

    @Operation(summary = "创建文章")
    @PostMapping
    public R<Void> create(@Valid @RequestBody KbArticleDTO dto) {
        articleService.create(dto, SecurityUtils.getLoginUser().getUserId(), SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "修改文章")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody KbArticleDTO dto) {
        articleService.update(id, dto, SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "修改成功");
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return R.ok(null, "删除成功");
    }

    @Operation(summary = "发布/审核文章")
    @PostMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        articleService.publish(id, body.get("status"), SecurityUtils.getLoginUser().getUsername());
        return R.ok(null, "状态已更新");
    }

    @Operation(summary = "已关闭工单转知识")
    @PostMapping("/from-ticket/{ticketId}")
    public R<KbArticleVO> fromTicket(@PathVariable Long ticketId) {
        return R.ok(articleService.fromTicket(ticketId,
                SecurityUtils.getLoginUser().getUserId(), SecurityUtils.getLoginUser().getUsername()));
    }
}