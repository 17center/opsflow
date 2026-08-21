package com.opsflow.module.cmdb.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 资产关联请求
 */
@Data
@Schema(description = "资产关联请求")
public class CmdbRelationDTO {

    @Schema(description = "源资产类型: HOST/SERVICE")
    @NotBlank(message = "源资产类型不能为空")
    private String sourceType;

    @Schema(description = "源资产 ID")
    @NotNull(message = "源资产 ID 不能为空")
    private Long sourceId;

    @Schema(description = "目标资产类型: HOST/SERVICE")
    @NotBlank(message = "目标资产类型不能为空")
    private String targetType;

    @Schema(description = "目标资产 ID")
    @NotNull(message = "目标资产 ID 不能为空")
    private Long targetId;

    @Schema(description = "关系类型: DEPLOYED_ON/DEPENDS_ON/CONTAINS")
    @NotBlank(message = "关系类型不能为空")
    private String relationType;
}