package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook回调日志实体（ISSUE-012）
 */
@Data
@TableName("wf_webhook_log")
public class WebhookLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联Webhook ID */
    private Long webhookId;

    /** Webhook Key */
    private String webhookKey;

    /** 触发事件类型 */
    private String eventType;

    /** 关联流程实例ID */
    private Long processInstanceId;

    /** 请求URL */
    private String requestUrl;

    /** 请求方法 */
    private String requestMethod;

    /** 请求体 */
    private String requestBody;

    /** 响应状态码 */
    private Integer responseStatus;

    /** 响应体 */
    private String responseBody;

    /** 状态：0-失败，1-成功，2-重试中 */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 触发时间 */
    private LocalDateTime triggerTime;

    /** 完成时间 */
    private LocalDateTime completeTime;
}
