package com.flow.engine.dto;

import lombok.Data;

/**
 * 创建流程定义请求
 */
@Data
public class ProcessDefinitionCreateRequest {

    /** 流程Key（必填） */
    private String processKey;

    /** 流程名称（必填） */
    private String processName;

    /** 分类 */
    private String category;

    /** 流程定义JSON（必填） */
    private String processJson;

    /** 创建人 */
    private String createBy;
}
