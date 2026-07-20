package com.flow.engine.common;

import lombok.Getter;

/**
 * 统一错误码。0 表示成功，4xx 为客户端类错误，5xx 为系统错误，1xxx 为业务错误。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_INVALID(400, "参数校验失败"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常"),
    BUSINESS_ERROR(1000, "业务异常"),
    PROCESS_DEF_NOT_FOUND(1001, "流程定义不存在"),
    PROCESS_KEY_DUPLICATE(1002, "流程定义Key已存在"),
    PROCESS_JSON_INVALID(1003, "流程定义JSON格式错误"),
    PROCESS_MISSING_START_NODE(1004, "流程定义缺少开始节点"),
    PROCESS_MISSING_END_NODE(1005, "流程定义缺少结束节点"),
    PROCESS_NODE_COUNT_EXCEED(1006, "节点数量超过上限500"),
    PROCESS_DEF_ALREADY_DEPLOYED(1007, "流程定义已部署"),
    PROCESS_INSTANCE_NOT_FOUND(1008, "流程实例不存在"),
    PROCESS_NOT_DEPLOYED(1009, "流程定义未部署"),
    STATUS_TRANSITION_INVALID(1010, "状态不允许此操作"),
    TASK_NOT_FOUND(1011, "任务不存在"),
    OPTIMISTIC_LOCK_CONFLICT(1012, "乐观锁冲突，数据已被修改"),
    TASK_ALREADY_CLAIMED(1013, "任务已被签收"),
    TASK_NOT_ASSIGNEE(1014, "非任务处理人，无权操作"),
    TASK_NOT_PENDING(1015, "任务不在待处理状态"),
    MODEL_KEY_DUPLICATE(1016, "数据模型Key已存在"),
    MODEL_NOT_FOUND(1017, "数据模型不存在"),
    MODEL_ALREADY_PUBLISHED(1018, "数据模型已发布，不可重复发布"),
    MODEL_VALIDATION_FAILED(1019, "数据模型校验失败"),
    MODEL_INSTANCE_NOT_FOUND(1020, "模型实例不存在"),
    MODEL_SUB_TABLE_EXCEED(1021, "子表数量超过上限10"),
    MODEL_FIELD_EXCEED(1022, "子表字段数量超过上限50"),
    WEBHOOK_KEY_DUPLICATE(1023, "Webhook Key已存在"),
    WEBHOOK_NOT_FOUND(1024, "Webhook配置不存在"),
    WEBHOOK_URL_INVALID(1025, "Webhook回调地址无效"),
    WEBHOOK_TRIGGER_FAILED(1026, "Webhook触发失败"),
    DEPT_NOT_FOUND(1027, "部门不存在"),
    DEPT_HAS_USERS(1028, "部门下存在用户，无法删除"),
    DEPT_HAS_CHILDREN(1029, "部门下存在子部门，无法删除"),
    USER_NOT_FOUND(1030, "用户不存在"),
    USERNAME_DUPLICATE(1031, "用户名已存在"),
    ROLE_NOT_FOUND(1032, "角色不存在"),
    ROLE_KEY_DUPLICATE(1033, "角色Key已存在"),
    PERMISSION_NOT_FOUND(1034, "权限不存在"),
    SECURITY_LEVEL_DENIED(1035, "密级不足，拒绝访问"),
    PERMISSION_DENIED(1036, "无权限访问"),
    TOKEN_INVALID(1037, "Token无效或已过期"),
    TOKEN_EXPIRED(1038, "Token已过期"),
    DICT_TYPE_NOT_FOUND(1039, "字典类型不存在"),
    DICT_CODE_DUPLICATE(1040, "字典编码已存在"),
    DICT_ITEM_NOT_FOUND(1041, "字典项不存在"),
    DICT_TYPE_BUILTIN(1042, "系统内置字典不可删除"),
    ADMIN_NOT_FOUND(1043, "管理员不存在"),
    ADMIN_CANNOT_DELETE_SELF(1044, "不能删除自己的账号"),
    ADMIN_CANNOT_OPERATE_OTHER(1045, "三员之间不能互相操作"),
    ADMIN_OPERATION_DENIED(1046, "权限不足，拒绝操作"),
    INSTANCE_NOT_FOUND(1047, "流程实例不存在"),
    INSTANCE_NOT_RUNNING(1048, "流程实例不在运行状态"),
    NODE_NOT_FOUND(1049, "节点不存在"),
    INTERVENE_FAILED(1050, "干预操作失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
