package com.flow.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook回调日志响应DTO（ISSUE-012）
 */
@Data
public class WebhookLogResponse {

    private Long id;
    private Long webhookId;
    private String webhookKey;
    private String eventType;
    private Long processInstanceId;
    private String requestUrl;
    private String requestMethod;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private Integer status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime triggerTime;
    private LocalDateTime completeTime;
}
