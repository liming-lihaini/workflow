package com.flow.engine.dto;

import lombok.Data;

import java.util.Map;

/**
 * 模型实例创建/更新请求（ISSUE-010）
 */
@Data
public class ModelInstanceRequest {

    /** 模型Key */
    private String modelKey;

    /** 关联流程实例ID（可选） */
    private Long processInstanceId;

    /** 实例数据（主表字段值 + 子表行数据） */
    private Map<String, Object> data;
}
