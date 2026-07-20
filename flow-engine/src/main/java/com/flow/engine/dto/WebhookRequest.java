package com.flow.engine.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Webhook配置请求DTO（ISSUE-012）
 */
@Data
public class WebhookRequest {

    /** Webhook唯一标识 */
    private String webhookKey;

    /** 名称 */
    private String name;

    /** 回调URL */
    private String url;

    /** HTTP方法：GET/POST/PUT */
    private String method;

    /** 请求头 */
    private Map<String, String> headers;

    /** 载荷模板JSON */
    private String payloadTemplate;

    /** 超时时间(毫秒) */
    private Integer timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 触发事件列表 */
    private List<String> triggerEvents;

    /** 关联流程定义Key */
    private String processKey;

    /** 关联节点ID */
    private String nodeId;
}
