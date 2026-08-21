package com.opsflow.module.workflow.model.vo;

import com.opsflow.module.workflow.model.dto.WfNodeDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程定义 VO
 */
@Data
public class WfDefinitionVO {

    private Long id;

    private String name;

    private String key;

    private Integer version;

    /** 状态：0=草稿 1=已发布 2=已停用 */
    private Integer status;

    private String statusName;

    private String description;

    private List<WfNodeDTO> nodes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}