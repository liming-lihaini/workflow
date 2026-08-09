package com.flow.engine.controllers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.Result;
import com.flow.engine.dto.WebhookLogResponse;
import com.flow.engine.dto.WebhookRequest;
import com.flow.engine.dto.WebhookResponse;
import com.flow.engine.entity.Webhook;
import com.flow.engine.entity.WebhookLog;
import com.flow.engine.mapper.WebhookLogMapper;
import com.flow.engine.service.EmsRetainService;
import com.flow.engine.service.WebhookScheduler;
import com.flow.engine.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Webhook回调API（ISSUE-012）
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookScheduler webhookScheduler;
    private final WebhookLogMapper webhookLogMapper;
    private final EmsRetainService retainService;

    /**
     * 修改留样状态的 Webhook 回调接口
     * <p>
     * 接收 { retainNo, status }，按留样编号更新留样状态。
     * 设计为可被「编号 LYXHSQ 的留样销毁流程」节点 Webhook 调用：
     * 流程节点完成时，引擎回调此接口并携带流程变量 formData.retainNo，
     * 由 webhook 配置的 payloadTemplate 拼装为 { retainNo, status }。
     */
    @PostMapping("/retain-status")
    public Result<Map<String, Object>> updateRetainStatus(@RequestBody Map<String, Object> body) {
        String retainNo = body == null ? null : (String) body.get("retainNo");
        String status = body == null ? null : (String) body.get("status");
        if (!StringUtils.hasText(retainNo) || !StringUtils.hasText(status)) {
            return Result.fail(400, "retainNo 与 status 均为必填");
        }
        int updated = retainService.updateStatusByRetainNo(retainNo, status);
        Map<String, Object> result = new HashMap<>();
        result.put("retainNo", retainNo);
        result.put("status", status);
        result.put("updated", updated);
        return Result.ok(result);
    }

    /**
     * 创建Webhook配置
     */
    @PostMapping
    public Result<WebhookResponse> createWebhook(@RequestBody WebhookRequest request) {
        return Result.ok(webhookService.createWebhook(request));
    }

    /**
     * 获取Webhook配置
     */
    @GetMapping("/{webhookKey}")
    public Result<WebhookResponse> getWebhook(@PathVariable String webhookKey) {
        return Result.ok(webhookService.getWebhook(webhookKey));
    }

    /**
     * 获取Webhook列表
     */
    @GetMapping
    public Result<List<WebhookResponse>> listWebhooks(@RequestParam(required = false) String processKey) {
        return Result.ok(webhookService.listWebhooks(processKey));
    }

    /**
     * 更新Webhook配置
     */
    @PutMapping("/{webhookKey}")
    public Result<WebhookResponse> updateWebhook(@PathVariable String webhookKey, @RequestBody WebhookRequest request) {
        return Result.ok(webhookService.updateWebhook(webhookKey, request));
    }

    /**
     * 删除Webhook配置
     */
    @DeleteMapping("/{webhookKey}")
    public Result<Void> deleteWebhook(@PathVariable String webhookKey) {
        webhookService.deleteWebhook(webhookKey);
        return Result.ok();
    }

    /**
     * 测试Webhook连通性（不执行业务，仅探测目标URL是否可达）
     */
    @PostMapping("/{webhookKey}/test")
    public Result<Map<String, Object>> testWebhook(@PathVariable String webhookKey) {
        Webhook webhook = webhookService.getWebhookEntityByKey(webhookKey);
        if (webhook == null) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_FOUND);
        }
        Map<String, Object> probe = new HashMap<>();
        probe.put("triggerType", "CONNECTIVITY_TEST");
        probe.put("test", true);
        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "CONNECTIVITY_TEST", null, probe);
        Map<String, Object> result = new HashMap<>();
        result.put("success", log.getStatus() != null && log.getStatus() == 1);
        result.put("responseStatus", log.getResponseStatus());
        result.put("errorMessage", log.getErrorMessage());
        result.put("url", webhook.getUrl());
        return Result.ok(result);
    }

    /**
     * 手动触发Webhook回调
     */
    @PostMapping("/{webhookId}/trigger")
    public Result<WebhookLogResponse> triggerWebhook(@PathVariable Long webhookId, @RequestBody(required = false) Map<String, Object> payload) {
        Webhook webhook = webhookService.getWebhookById(webhookId);
        if (webhook == null) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_FOUND);
        }

        if (payload == null) {
            payload = new HashMap<>();
        }
        payload.putIfAbsent("triggerType", "MANUAL");

        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "MANUAL_TRIGGER", null, payload);
        return Result.ok(toLogResponse(log));
    }

    /**
     * 获取回调日志
     */
    @GetMapping("/logs")
    public Result<List<WebhookLogResponse>> getWebhookLogs(
            @RequestParam(required = false) String webhookKey,
            @RequestParam(required = false) Long processInstanceId,
            @RequestParam(required = false) String eventType) {

        LambdaQueryWrapper<WebhookLog> wrapper = new LambdaQueryWrapper<>();
        if (webhookKey != null) {
            wrapper.eq(WebhookLog::getWebhookKey, webhookKey);
        }
        if (processInstanceId != null) {
            wrapper.eq(WebhookLog::getProcessInstanceId, processInstanceId);
        }
        if (eventType != null) {
            wrapper.eq(WebhookLog::getEventType, eventType);
        }
        wrapper.orderByDesc(WebhookLog::getTriggerTime);

        List<WebhookLogResponse> logs = webhookLogMapper.selectList(wrapper).stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());

        return Result.ok(logs);
    }

    /**
     * 重试回调
     */
    @PostMapping("/logs/{logId}/retry")
    public Result<WebhookLogResponse> retryWebhook(@PathVariable Long logId) {
        WebhookLog log = webhookScheduler.retryWebhook(logId);
        return Result.ok(toLogResponse(log));
    }

    private WebhookLogResponse toLogResponse(WebhookLog log) {
        WebhookLogResponse response = new WebhookLogResponse();
        response.setId(log.getId());
        response.setWebhookId(log.getWebhookId());
        response.setWebhookKey(log.getWebhookKey());
        response.setEventType(log.getEventType());
        response.setProcessInstanceId(log.getProcessInstanceId());
        response.setRequestUrl(log.getRequestUrl());
        response.setRequestMethod(log.getRequestMethod());
        response.setRequestBody(log.getRequestBody());
        response.setResponseStatus(log.getResponseStatus());
        response.setResponseBody(log.getResponseBody());
        response.setStatus(log.getStatus());
        response.setRetryCount(log.getRetryCount());
        response.setErrorMessage(log.getErrorMessage());
        response.setTriggerTime(log.getTriggerTime());
        response.setCompleteTime(log.getCompleteTime());
        return response;
    }
}
