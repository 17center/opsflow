package com.opsflow.common.enums;

import lombok.Getter;

/**
 * 业务错误码
 * 错误码规则见 API 接口设计文档第 17 章
 */
@Getter
public enum ErrorCode {

    // ===== 通用 =====
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务端内部错误"),

    // ===== 认证模块 (400xx) =====
    LOGIN_FAILED(40001, "用户名或密码错误"),
    ACCOUNT_LOCKED(40002, "账号已被锁定"),
    TOKEN_EXPIRED(40003, "Token 已过期"),
    TOKEN_INVALID(40004, "Token 无效"),
    NO_PERMISSION(40005, "无权限访问"),

    // ===== 用户模块 (4001x) =====
    USERNAME_EXISTS(40010, "用户名已存在"),
    ADMIN_NOT_DELETABLE(40011, "不可删除超级管理员"),
    SELF_NOT_DELETABLE(40012, "不可删除自己"),
    ADMIN_NOT_DISABLED(40013, "不可禁用超级管理员"),
    USER_NOT_FOUND(40014, "用户不存在"),

    // ===== 角色模块 (4002x) =====
    ROLE_CODE_EXISTS(40020, "角色编码已存在"),
    ROLE_HAS_USERS(40021, "角色下存在关联用户"),
    ROLE_NOT_FOUND(40022, "角色不存在"),
    ROLE_ADMIN_PROTECTED(40023, "内置管理员角色不可删除/停用"),

    // ===== 菜单模块 (4003x) =====
    MENU_HAS_CHILDREN(40030, "菜单下存在子菜单"),
    MENU_NOT_FOUND(40031, "菜单不存在"),
    MENU_PERMISSION_EXISTS(40032, "权限标识已存在"),
    MENU_PARENT_INVALID(40033, "父菜单不能为自身或子菜单"),

    // ===== 部门模块 (4004x) =====
    DEPT_HAS_USERS(40040, "部门下存在用户"),
    DEPT_HAS_CHILDREN(40041, "部门下存在子部门"),
    DEPT_NOT_FOUND(40042, "部门不存在"),
    DEPT_PARENT_INVALID(40043, "父部门不能为自身或子部门"),

    // ===== 工单模块 (4005x) =====
    TICKET_STATUS_NOT_ALLOWED(40050, "工单状态不允许此操作"),
    TICKET_NO_WORKFLOW(40051, "工单未关联流程模板"),
    TICKET_NO_PERMISSION(40052, "无权操作此工单"),
    ATTACHMENT_TOO_LARGE(40053, "附件大小超过限制(20MB)"),

    // ===== 工作流模块 (40054-40064) =====
    WF_KEY_EXISTS(40054, "流程标识已存在"),
    WF_NOT_FOUND(40055, "流程定义不存在"),
    WF_PUBLISHED_NOT_EDIT(40056, "已发布的流程不可修改"),
    WF_ALREADY_PUBLISHED(40057, "流程已发布"),
    WF_ALREADY_DISABLED(40058, "流程已停用"),
    WF_TASK_NOT_FOUND(40059, "审批任务不存在"),
    WF_TASK_DONE(40061, "审批任务已完成"),
    WF_TASK_NO_PERMISSION(40062, "无权处理该审批任务"),
    WF_INSTANCE_NOT_RUNNING(40063, "流程实例未在运行中"),
    WF_TICKET_ALREADY_PROCESS(40064, "工单已存在审批流程"),
    WF_NO_APPROVAL_NODE(40065, "流程定义未配置人工审批节点"),

    // ===== 自动化模块 (40060, 40070-40079) =====
    SCRIPT_BOUND_TEMPLATE(40060, "脚本已关联工单模板"),
    HOST_UNREACHABLE(40070, "目标主机不可达"),
    HOST_BUSY(40071, "该主机有脚本正在执行"),
    EXEC_COMPLETED(40072, "任务已完成，无法终止"),
    SCRIPT_NOT_FOUND(40073, "脚本不存在"),
    SCRIPT_DISABLED(40074, "脚本已停用"),
    SCRIPT_VERSION_NOT_FOUND(40075, "脚本版本不存在"),
    HOST_NOT_FOUND(40076, "目标主机不存在"),
    EXEC_NOT_FOUND(40077, "执行记录不存在"),
    HOST_CRED_INVALID(40078, "目标主机认证配置无效"),

    // ===== 告警模块 (4008x) =====
    ALERT_RULE_EXISTS(40080, "告警规则已存在(同指标同主机)"),
    ALERT_RULE_NOT_FOUND(40081, "告警规则不存在"),
    ALERT_EVENT_NOT_FOUND(40082, "告警事件不存在"),
    ALERT_EVENT_ALREADY_CONFIRMED(40083, "告警事件已确认"),
    ALERT_DUTY_EXISTS(40084, "值班排班已存在(同一人同日同班次)"),
    ALERT_DUTY_NOT_FOUND(40085, "值班排班不存在"),

    // ===== CMDB 资产模块 (40090-40099) =====
    SERVICE_NOT_FOUND(40090, "服务资产不存在"),
    SERVICE_HOST_NOT_FOUND(40091, "服务所在主机不存在"),
    RELATION_EXISTS(40092, "资产关联关系已存在"),
    RELATION_INVALID(40093, "资产关联关系无效(源或目标资产不存在)"),
    RELATION_SELF(40094, "资产不能关联自身"),
    RELATION_NOT_FOUND(40095, "资产关联关系不存在"),
    SERVICE_TYPE_INVALID(40096, "服务类型无效"),

    // ===== 知识库模块 (4010x) =====
    KB_ARTICLE_NOT_FOUND(40100, "知识文章不存在"),
    KB_TAG_EXISTS(40101, "标签已存在"),
    KB_TAG_NOT_FOUND(40102, "标签不存在"),
    KB_TAG_HAS_ARTICLES(40103, "标签下存在关联文章"),
    KB_TICKET_INVALID(40104, "工单不允许转为知识(仅已关闭工单)"),

    // ===== 数据报表模块 (4011x) =====
    REPORT_TYPE_INVALID(40110, "报表类型无效"),
    REPORT_FORMAT_INVALID(40111, "导出格式无效(仅支持 EXCEL/PDF)"),
    REPORT_TIME_RANGE_INVALID(40112, "时间范围无效(开始时间不能晚于结束时间)"),
    REPORT_NOT_FOUND(40113, "报表文件不存在")
    ;

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}