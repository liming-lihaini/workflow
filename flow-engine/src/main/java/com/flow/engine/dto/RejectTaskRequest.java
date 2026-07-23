package com.flow.engine.dto;

import lombok.Data;

import java.util.Map;

/**
 * 驳回任务请求
 */
@Data
public class RejectTaskRequest {

    /** 操作人ID */
    private String userId;

    /** 驳回意见 */
    private String comment;

    /** 回退目标节点ID（可选，为空时回退到上一个 userTask 节点） */
    private String targetNodeId;

    /** 附加变量 */
    private Map<String, Object> variables;
}
