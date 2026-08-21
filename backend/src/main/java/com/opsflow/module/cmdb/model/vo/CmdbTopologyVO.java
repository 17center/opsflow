package com.opsflow.module.cmdb.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 资产拓扑图数据 VO
 * 节点分 HOST / SERVICE 两类，边表示资产间关联关系
 */
@Data
public class CmdbTopologyVO {

    /** 拓扑节点 */
    private List<Node> nodes;

    /** 拓扑边 */
    private List<Edge> edges;

    @Data
    public static class Node {
        private String id;
        /** HOST | SERVICE */
        private String type;
        private String name;
        private String label;
        /** 状态：0=不可用 1=运行中 2=维护中 3=已退役 */
        private Integer status;
        /** 归属主机 ID（服务节点） */
        private Long hostId;
    }

    @Data
    public static class Edge {
        private String source;
        private String target;
        /** DEPLOYED_ON | DEPENDS_ON | CONTAINS */
        private String relationType;
    }
}