package com.opsflow.module.automation.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 执行详情 VO（记录 + 输出日志）
 */
@Data
public class AutoExecDetailVO {

    private AutoExecRecordVO record;

    private List<AutoExecLogVO> logs;
}