package com.flow.engine.service;

import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.entity.Webhook;
import com.flow.engine.entity.WebhookLog;
import com.flow.engine.mapper.WebhookLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Webhook调度器（ISSUE-012）
 * <p>
 * 负责触发、重试和异步调度Webhook回调。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookScheduler {

    private final WebhookLogMapper webhookLogMapper;
    private final RestTemplate webhookRestTemplate;

    /**
     * 同步触发Webhook
     */
    public WebhookLog triggerWebhook(Webhook webhook, String eventType, Long processInstanceId, Map<String, Object> payload) {
        WebhookLog webhookLog = createLog(webhook, eventType, processInstanceId, payload);

        try {
            // 构建请求
            HttpHeaders headers = buildHeaders(webhook);
            String body = buildBody(webhook, payload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            HttpMethod method = HttpMethod.valueOf(webhook.getMethod().toUpperCase());
            ResponseEntity<String> response = webhookRestTemplate.exchange(
                    buildEncodedUri(webhook.getUrl()), method, entity, String.class
            );

            // 记录成功
            webhookLog.setResponseStatus(response.getStatusCode().value());
            webhookLog.setResponseBody(response.getBody());
            webhookLog.setStatus(1); // 成功
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLogMapper.updateById(webhookLog);

            log.info("Webhook触发成功: key={}, url={}, status={}", webhook.getWebhookKey(), webhook.getUrl(), response.getStatusCode().value());

        } catch (ResourceAccessException e) {
            // 超时或连接失败
            webhookLog.setStatus(0); // 失败
            webhookLog.setErrorMessage("连接超时或失败: " + e.getMessage());
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLogMapper.updateById(webhookLog);

            log.warn("Webhook触发失败(超时): key={}, url={}", webhook.getWebhookKey(), webhook.getUrl());

            // 触发重试
            if (webhook.getRetryCount() > 0) {
                retryWebhook(webhookLog, webhook);
            }

        } catch (HttpStatusCodeException e) {
            // 目标服务返回非 2xx（含 5xx 瞬时错误，如 SQLite SQLITE_BUSY 导致的 500）
            webhookLog.setStatus(0);
            webhookLog.setErrorMessage("HTTP " + e.getStatusCode().value() + " 错误: " + e.getResponseBodyAsString());
            webhookLog.setResponseStatus(e.getStatusCode().value());
            webhookLog.setResponseBody(e.getResponseBodyAsString());
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLogMapper.updateById(webhookLog);

            // 5xx 视为可重试的瞬时错误（如数据库锁冲突），4xx 为业务错误不重试
            if (e.getStatusCode().is5xxServerError() && webhook.getRetryCount() > 0) {
                log.warn("Webhook触发返回5xx，准备重试: key={}, status={}", webhook.getWebhookKey(), e.getStatusCode().value());
                retryWebhook(webhookLog, webhook);
            } else {
                log.error("Webhook触发HTTP错误(不重试): key={}, status={}", webhook.getWebhookKey(), e.getStatusCode().value());
            }

        } catch (Exception e) {
            webhookLog.setStatus(0);
            webhookLog.setErrorMessage("触发异常: " + e.getMessage());
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLogMapper.updateById(webhookLog);

            log.error("Webhook触发异常: key={}, error={}", webhook.getWebhookKey(), e.getMessage());
        }

        return webhookLog;
    }

    /**
     * 重试Webhook
     */
    public WebhookLog retryWebhook(Long webhookLogId) {
        WebhookLog webhookLog = webhookLogMapper.selectById(webhookLogId);
        if (webhookLog == null) {
            throw new RuntimeException("Webhook日志不存在: " + webhookLogId);
        }

        // 获取webhook配置
        Webhook webhook = new Webhook();
        webhook.setUrl(webhookLog.getRequestUrl());
        webhook.setMethod(webhookLog.getRequestMethod());
        webhook.setRetryCount(1);

        // 重试
        webhookLog.setStatus(2); // 重试中
        webhookLog.setRetryCount(webhookLog.getRetryCount() + 1);
        webhookLogMapper.updateById(webhookLog);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(webhookLog.getRequestBody(), headers);

            HttpMethod method = HttpMethod.valueOf(webhookLog.getRequestMethod().toUpperCase());
            ResponseEntity<String> response = webhookRestTemplate.exchange(
                    buildEncodedUri(webhookLog.getRequestUrl()), method, entity, String.class
            );

            webhookLog.setResponseStatus(response.getStatusCode().value());
            webhookLog.setResponseBody(response.getBody());
            webhookLog.setStatus(1); // 成功
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLog.setErrorMessage(null);
            webhookLogMapper.updateById(webhookLog);

            log.info("Webhook重试成功: logId={}", webhookLogId);

        } catch (Exception e) {
            webhookLog.setStatus(0);
            webhookLog.setErrorMessage("重试失败: " + e.getMessage());
            webhookLog.setCompleteTime(LocalDateTime.now());
            webhookLogMapper.updateById(webhookLog);

            log.warn("Webhook重试失败: logId={}, error={}", webhookLogId, e.getMessage());
        }

        return webhookLog;
    }

    /**
     * 异步调度Webhook
     */
    @Async
    public void scheduleAsyncWebhook(Webhook webhook, String eventType, Long processInstanceId, Map<String, Object> payload) {
        log.info("异步调度Webhook: key={}, event={}", webhook.getWebhookKey(), eventType);
        triggerWebhook(webhook, eventType, processInstanceId, payload);
    }

    private WebhookLog createLog(Webhook webhook, String eventType, Long processInstanceId, Map<String, Object> payload) {
        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setWebhookId(webhook.getId());
        webhookLog.setWebhookKey(webhook.getWebhookKey());
        webhookLog.setEventType(eventType);
        webhookLog.setProcessInstanceId(processInstanceId);
        webhookLog.setRequestUrl(webhook.getUrl());
        webhookLog.setRequestMethod(webhook.getMethod());
        webhookLog.setRequestBody(buildBody(webhook, payload));
        webhookLog.setStatus(0);
        webhookLog.setRetryCount(0);
        webhookLog.setTriggerTime(LocalDateTime.now());
        webhookLogMapper.insert(webhookLog);
        return webhookLog;
    }

    private HttpHeaders buildHeaders(Webhook webhook) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (webhook.getHeaders() != null) {
            try {
                Map<String, String> customHeaders = JsonUtils.fromJson(webhook.getHeaders(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                customHeaders.forEach(headers::set);
            } catch (Exception e) {
                log.warn("解析Webhook请求头失败: key={}", webhook.getWebhookKey());
            }
        }
        return headers;
    }

    /** 对含中文等非法字符的 URL 做 URI 编码（支持路径参数中的中文），避免 RestTemplate 解析失败 */
    private java.net.URI buildEncodedUri(String url) {
        try {
            return org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url).build().toUri();
        } catch (Exception e) {
            return java.net.URI.create(url);
        }
    }

    private String buildBody(Webhook webhook, Map<String, Object> payload) {
        if (webhook.getPayloadTemplate() != null && !webhook.getPayloadTemplate().isEmpty()) {
            // 模板替换，支持嵌套路径：${a.b.c} 与 ${a}
            String body = webhook.getPayloadTemplate();
            java.util.regex.Matcher matcher = PAYLOAD_TEMPLATE_PATTERN.matcher(body);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                Object val = resolveTemplatePath(payload, matcher.group(1));
                matcher.appendReplacement(sb, val == null ? "" : java.util.regex.Matcher.quoteReplacement(String.valueOf(val)));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
        // 默认使用payload JSON
        return payload != null ? JsonUtils.toJson(payload) : "{}";
    }

    private static final java.util.regex.Pattern PAYLOAD_TEMPLATE_PATTERN =
            java.util.regex.Pattern.compile("\\$\\{([\\w.]+)\\}");

    /** 按点号路径（a.b.c）从 payload Map 中取值，支持嵌套 Map */
    private Object resolveTemplatePath(Map<String, Object> payload, String path) {
        if (payload == null || path == null || path.isEmpty()) return null;
        String[] parts = path.split("\\.");
        Object cur = payload;
        for (String part : parts) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(part);
            if (cur == null) return null;
        }
        return cur;
    }

    private void retryWebhook(WebhookLog webhookLog, Webhook webhook) {
        int maxRetry = webhook.getRetryCount();
        int currentRetry = 0;

        while (currentRetry < maxRetry) {
            currentRetry++;
            webhookLog.setStatus(2); // 重试中
            webhookLog.setRetryCount(currentRetry);
            webhookLogMapper.updateById(webhookLog);

            try {
                HttpHeaders headers = buildHeaders(webhook);
                HttpEntity<String> entity = new HttpEntity<>(webhookLog.getRequestBody(), headers);
                HttpMethod method = HttpMethod.valueOf(webhook.getMethod().toUpperCase());

                ResponseEntity<String> response = webhookRestTemplate.exchange(
                        webhook.getUrl(), method, entity, String.class
                );

                webhookLog.setResponseStatus(response.getStatusCode().value());
                webhookLog.setResponseBody(response.getBody());
                webhookLog.setStatus(1); // 成功
                webhookLog.setCompleteTime(LocalDateTime.now());
                webhookLog.setErrorMessage(null);
                webhookLogMapper.updateById(webhookLog);

                log.info("Webhook重试成功: logId={}, retryCount={}", webhookLog.getId(), currentRetry);
                return;

            } catch (Exception e) {
                log.warn("Webhook重试第{}次失败: logId={}, error={}", currentRetry, webhookLog.getId(), e.getMessage());
            }
        }

        // 所有重试都失败
        webhookLog.setStatus(0);
        webhookLog.setErrorMessage("重试" + maxRetry + "次后仍失败");
        webhookLog.setCompleteTime(LocalDateTime.now());
        webhookLogMapper.updateById(webhookLog);
    }
}
