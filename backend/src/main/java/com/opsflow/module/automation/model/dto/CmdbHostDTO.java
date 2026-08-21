package com.opsflow.module.automation.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 目标主机请求
 */
@Data
@Schema(description = "目标主机请求")
public class CmdbHostDTO {

    @Schema(description = "主机名")
    @NotBlank(message = "主机名不能为空")
    @Size(max = 128, message = "主机名长度不能超过 128")
    private String hostname;

    @Schema(description = "IP 地址")
    @NotBlank(message = "IP 地址不能为空")
    private String ipAddress;

    @Schema(description = "SSH 端口")
    @NotNull(message = "SSH 端口不能为空")
    @Min(value = 1, message = "端口无效")
    @Max(value = 65535, message = "端口无效")
    private Integer sshPort;

    @Schema(description = "SSH 登录用户名，为空时默认 root")
    private String sshUser;

    @Schema(description = "操作系统类型")
    private String osType;

    @Schema(description = "操作系统版本")
    private String osVersion;

    @Schema(description = "CPU 核数")
    private Integer cpuCores;

    @Schema(description = "内存(GB)")
    private Integer memoryGb;

    @Schema(description = "磁盘(GB)")
    private Integer diskGb;

    @Schema(description = "认证方式：1=密码 2=密钥")
    @NotNull(message = "认证方式不能为空")
    private Integer authType;

    @Schema(description = "凭据（密码明文或私钥内容，回显时加密存储）")
    @NotBlank(message = "凭据不能为空")
    private String credential;

    @Schema(description = "负责人 ID")
    private Long ownerId;

    @Schema(description = "主机分组")
    private String groupName;

    @Schema(description = "备注")
    private String remark;
}