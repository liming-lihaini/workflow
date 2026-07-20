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
    private String processKey;
    private String processName;
    private Integer processVersion;
    private String businessKey;
    private Integer status;
    private String statusDesc;
    private String currentNodeId;
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
