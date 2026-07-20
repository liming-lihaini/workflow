package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Webhook配置响应DTO（ISSUE-012）
 */
@Data
public class WebhookResponse {

    private Long id;
    private String webhookKey;
    private String name;
    private String url;
    private String method;
    private Map<String, String> headers;
    private String payloadTemplate;
    private Integer timeout;
    private Integer retryCount;
    private List<String> triggerEvents;
    private String processKey;
    private String nodeId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
