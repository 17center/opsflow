package com.opsflow.module.cmdb.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产关联关系 VO
 */
@Data
public class CmdbRelationVO {

    private Long id;

    /** 源资产类型: HOST/SERVICE */
    private String sourceType;

    private String sourceTypeName;

    /** 源资产 ID */
    private Long sourceId;

    /** 源资产名称 */
    private String sourceName;

    /** 目标资产类型: HOST/SERVICE */
    private String targetType;

    private String targetTypeName;

    /** 目标资产 ID */
    private Long targetId;

    /** 目标资产名称 */
    private String targetName;

    /** 关系类型: DEPLOYED_ON/DEPENDS_ON/CONTAINS */
    private String relationType;

    private String relationTypeName;

    private LocalDateTime createTime;
}