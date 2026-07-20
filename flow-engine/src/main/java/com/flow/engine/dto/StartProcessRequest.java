package com.flow.engine.dto;

import lombok.Data;

import java.util.Map;

/**
 * 发起流程请求
 */
@Data
public class StartProcessRequest {

    /** 流程定义Key（必填） */
    private String processKey;

    /** 业务主键 */
    private String businessKey;

    /** 发起人 */
    private String startUser;

    /** 流程变量 */
    private Map<String, Object> variables;
}
