package com.flow.engine.dto;

import lombok.Data;

/**
 * 更新流程定义请求
 */
@Data
public class ProcessDefinitionUpdateRequest {

    /** 流程名称 */
    private String processName;

    /** 流程类型 */
    private String processType;

    /** 用途/描述 */
    private String description;

    /** 分类 */
    private String category;

    /** 流程定义JSON */
    private String processJson;
}
