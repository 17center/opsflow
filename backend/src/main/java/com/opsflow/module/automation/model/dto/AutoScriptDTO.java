package com.opsflow.module.automation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/更新脚本请求
 */
@Data
@Schema(description = "创建/更新脚本请求")
public class AutoScriptDTO {

    @Schema(description = "脚本名称")
    @NotBlank(message = "脚本名称不能为空")
    @Size(max = 128, message = "脚本名称长度不能超过 128")
    private String name;

    @Schema(description = "脚本说明")
    private String description;

    @Schema(description = "脚本类型：1=Shell 2=Python 3=Ansible")
    @NotNull(message = "脚本类型不能为空")
    private Integer scriptType;

    @Schema(description = "脚本内容")
    @NotBlank(message = "脚本内容不能为空")
    private String content;

    @Schema(description = "参数定义(JSON)")
    private String paramsSchema;

    @Schema(description = "默认超时时间(秒)")
    @NotNull(message = "超时时间不能为空")
    @Min(value = 1, message = "超时时间至少 1 秒")
    @Max(value = 3600, message = "超时时间最大 3600 秒")
    private Integer timeoutSeconds;

    @Schema(description = "脚本分类")
    private String category;

    @Schema(description = "变更说明(非首次创建时必填)")
    private String changeLog;
}