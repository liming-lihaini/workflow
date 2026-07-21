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

    /** 流程类型：approval-审批流，process-业务流程，callback-回调流程 */
    private String processType;

    /** 用途/描述 */
    private String description;

    /** 流程定义JSON（可选，允许先创建基本信息再设计流程图） */
    private String processJson;

    /** 创建人 */
    private String createBy;
}
