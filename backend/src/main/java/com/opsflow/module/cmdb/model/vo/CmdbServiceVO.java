package com.opsflow.module.cmdb.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务资产 VO
 */
@Data
public class CmdbServiceVO {

    private Long id;

    private String name;

    private String serviceType;

    private String version;

    private Long hostId;

    private String hostName;

    private String hostIp;

    private Integer port;

    /** 状态：0=不可用 1=运行中 2=维护中 */
    private Integer status;

    private String statusName;

    private Long ownerId;

    private String ownerName;

    private LocalDateTime createTime;

    private String remark;
}