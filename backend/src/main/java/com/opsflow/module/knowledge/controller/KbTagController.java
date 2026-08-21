package com.opsflow.module.knowledge.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.knowledge.model.dto.KbTagDTO;
import com.opsflow.module.knowledge.model.vo.KbTagVO;
import com.opsflow.module.knowledge.service.KbTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签接口
 */
@Tag(name = "知识库标签")
@RestController
@RequestMapping("/api/kb/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('kb:article:list')")
public class KbTagController {

    private final KbTagService tagService;

    @Operation(summary = "标签列表")
    @GetMapping
    public R<List<KbTagVO>> list() {
        return R.ok(tagService.list());
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public R<Void> create(@Valid @RequestBody KbTagDTO dto) {
        tagService.create(dto);
        return R.ok(null, "创建成功");
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok(null, "删除成功");
    }
}