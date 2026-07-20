package com.flow.engine.dto;

import lombok.Data;

import java.util.Map;

/**
 * 模型实例响应（ISSUE-010）
 */
@Data
public class ModelInstanceResponse {

    private Long id;
    private String modelKey;
    private String modelInstanceId;
    private Long processInstanceId;
    private Map<String, Object> data;
    private String createTime;
    private String updateTime;
}
