package com.flow.engine.common.enums;

import lombok.Getter;

/**
 * 节点类型枚举（ISSUE-011 内置节点类型 + 自定义扩展约定）。
 * nodeType 即 {@code NodeHandler#getNodeType()} 返回的标识，全局唯一。
 */
@Getter
public enum NodeType {

    START("start", "开始节点"),
    END("end", "结束节点"),
    USER_TASK("userTask", "用户任务"),
    SERVICE_TASK("serviceTask", "服务任务"),
    SCRIPT_TASK("scriptTask", "脚本任务"),
    EXCLUSIVE_GATEWAY("exclusiveGateway", "排他网关"),
    PARALLEL_GATEWAY("parallelGateway", "并行网关"),
    INCLUSIVE_GATEWAY("inclusiveGateway", "包容网关"),
    SUB_PROCESS("subProcess", "子流程"),
    CUSTOM("custom", "自定义节点");

    private final String code;
    private final String desc;

    NodeType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static NodeType fromCode(String code) {
        for (NodeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
