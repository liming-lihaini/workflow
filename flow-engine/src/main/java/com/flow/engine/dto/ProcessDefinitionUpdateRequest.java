package com.flow.engine.dto;

import lombok.Data;

/**
 * 更新流程定义请求
 */
@Data
public class ProcessDefinitionUpdateRequest {

    /** 流程名称 */
    private String processName;

    /** 分类 */
    private String category;

    /** 流程定义JSON */
    private String processJson;
}
