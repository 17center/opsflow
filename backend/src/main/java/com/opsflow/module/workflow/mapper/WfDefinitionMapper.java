package com.opsflow.module.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.opsflow.module.workflow.model.entity.WfDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 流程定义表 Mapper
 */
@Mapper
public interface WfDefinitionMapper extends BaseMapper<WfDefinition> {

    /**
     * 按流程标识统计定义数量（含逻辑删除行，key 全局唯一）
     * 说明：逻辑删除行仍占用 uk_key 唯一索引，故统计须包含已删除行，避免重复插入。
     */
    @Select("SELECT COUNT(*) FROM wf_definition WHERE `key` = #{key}")
    Long countByKey(@Param("key") String key);
}