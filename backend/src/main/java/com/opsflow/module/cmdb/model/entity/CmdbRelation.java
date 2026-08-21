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
 * 资产关联关系实体（cmdb_relation 表）
 */
@Data
@TableName("cmdb_relation")
public class CmdbRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源资产类型: HOST/SERVICE */
    private String sourceType;

    /** 源资产 ID */
    private Long sourceId;

    /** 目标资产类型: HOST/SERVICE */
    private String targetType;

    /** 目标资产 ID */
    private Long targetId;

    /** 关系类型: DEPLOYED_ON/DEPENDS_ON/CONTAINS */
    private String relationType;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}