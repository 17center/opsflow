package com.opsflow.module.cmdb.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 服务资产请求
 */
@Data
@Schema(description = "服务资产请求")
public class CmdbServiceDTO {

    @Schema(description = "服务名称")
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 128, message = "服务名称长度不能超过 128")
    private String name;

    @Schema(description = "服务类型(MySQL/Redis/Nginx)")
    @NotBlank(message = "服务类型不能为空")
    @Size(max = 32, message = "服务类型长度不能超过 32")
    private String serviceType;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "所在主机 ID")
    private Long hostId;

    @Schema(description = "服务端口")
    @Min(value = 1, message = "端口无效")
    @Max(value = 65535, message = "端口无效")
    private Integer port;

    @Schema(description = "状态：0=不可用 1=运行中 2=维护中")
    private Integer status;

    @Schema(description = "负责人 ID")
    private Long ownerId;

    @Schema(description = "备注")
    private String remark;
}