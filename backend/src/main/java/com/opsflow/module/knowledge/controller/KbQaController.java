package com.opsflow.module.knowledge.controller;

import com.opsflow.common.result.R;
import com.opsflow.module.knowledge.model.dto.KbQaDTO;
import com.opsflow.module.knowledge.model.vo.KbQaVO;
import com.opsflow.module.knowledge.service.KbQaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能问答接口
 */
@Tag(name = "智能问答")
@RestController
@RequestMapping("/api/kb/qa")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('kb:qa')")
public class KbQaController {

    private final KbQaService qaService;

    @Operation(summary = "智能问答（RAG）")
    @PostMapping
    public R<KbQaVO> ask(@Valid @RequestBody KbQaDTO dto) {
        return R.ok(qaService.ask(dto));
    }
}