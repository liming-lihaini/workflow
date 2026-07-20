package com.flow.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Webhook配置实体（ISSUE-012）
 */
@Data
@TableName("wf_webhook")
public class Webhook {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Webhook唯一标识 */
    private String webhookKey;

    /** 名称 */
    private String name;

    /** 回调URL */
    private String url;

    /** HTTP方法：GET/POST/PUT */
    private String method;

    /** 请求头JSON */
    private String headers;

    /** 载荷模板JSON */
    private String payloadTemplate;

    /** 超时时间(毫秒) */
    private Integer timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 触发事件列表JSON */
    private String triggerEvents;

    /** 关联流程定义Key */
    private String processKey;

    /** 关联节点ID */
    private String nodeId;

    /** 状态：0-停用，1-启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
