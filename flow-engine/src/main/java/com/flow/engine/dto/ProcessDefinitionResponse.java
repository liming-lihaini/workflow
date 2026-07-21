package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程定义响应
 */
@Data
public class ProcessDefinitionResponse {

    private Long id;
    private String processKey;
    private String processName;
    private Integer version;
    private String processJson;
    private String category;
    private String processType;
    private String description;
    private Integer status;
    private String deploymentId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
}
