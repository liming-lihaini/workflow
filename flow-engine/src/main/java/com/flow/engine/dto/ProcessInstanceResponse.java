package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程实例响应
 */
@Data
public class ProcessInstanceResponse {

    private Long id;
    /** 流程编号 */
    private String instanceNo;
    private String processKey;
    private String processName;
    /** 流程类型 */
    private String processType;
    private Integer processVersion;
    private String businessKey;
    private Integer status;
    private String statusDesc;
    private String currentNodeId;
    /** 当前节点中文名称 */
    private String currentNodeName;
    private String startUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 流程变量（仅在查询变量时填充） */
    private Map<String, Object> variables;
}
