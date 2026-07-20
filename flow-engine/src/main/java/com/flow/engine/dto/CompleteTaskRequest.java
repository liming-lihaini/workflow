package com.flow.engine.dto;

import lombok.Data;

import java.util.Map;

/**
 * 完成任务请求
 */
@Data
public class CompleteTaskRequest {

    /** 任务变量（会合并到流程变量） */
    private Map<String, Object> variables;

    /** 审批意见 */
    private String opinion;
}
