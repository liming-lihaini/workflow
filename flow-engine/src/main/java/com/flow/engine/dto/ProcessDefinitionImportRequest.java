package com.flow.engine.dto;

import lombok.Data;

/**
 * 导入流程定义请求
 */
@Data
public class ProcessDefinitionImportRequest {

    /** 流程Key */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 分类 */
    private String category;

    /** 流程定义JSON */
    private String processJson;

    /** 创建人 */
    private String createBy;
}
