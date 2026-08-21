package com.opsflow.module.cmdb.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务资产实体（cmdb_service 表）
 */
@Data
@TableName("cmdb_service")
public class CmdbService implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务名称 */
    private String name;

    /** 服务类型(MySQL/Redis/Nginx/Tomcat) */
    private String serviceType;

    /** 版本号 */
    private String version;

    /** 所在主机 ID */
    private Long hostId;

    /** 服务端口 */
    private Integer port;

    /** 状态：0=不可用 1=运行中 2=维护中 */
    private Integer status;

    /** 负责人 ID */
    private Long ownerId;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    private String remark;
}