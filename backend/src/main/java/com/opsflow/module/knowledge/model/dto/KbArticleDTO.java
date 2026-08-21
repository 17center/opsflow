package com.opsflow.module.knowledge.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 知识文章请求
 */
@Data
@Schema(description = "知识文章请求")
public class KbArticleDTO {

    @Schema(description = "文章标题")
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 256, message = "文章标题长度不能超过 256")
    private String title;

    @Schema(description = "文章内容（Markdown）")
    @NotBlank(message = "文章内容不能为空")
    private String content;

    @Schema(description = "分类：1=故障排查 2=操作手册 3=最佳实践 4=FAQ")
    @NotNull(message = "文章分类不能为空")
    @Min(value = 1, message = "分类无效")
    @Max(value = 4, message = "分类无效")
    private Integer category;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Schema(description = "状态：0=草稿 1=已发布 2=审核中")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}