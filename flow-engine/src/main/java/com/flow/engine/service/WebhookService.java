package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.WebhookRequest;
import com.flow.engine.dto.WebhookResponse;
import com.flow.engine.entity.Webhook;
import com.flow.engine.mapper.WebhookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Webhook配置管理服务（ISSUE-012）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookMapper webhookMapper;

    /**
     * 创建Webhook配置
     */
    public WebhookResponse createWebhook(WebhookRequest request) {
        // 校验webhookKey唯一性
        Webhook existing = webhookMapper.selectOne(
                new LambdaQueryWrapper<Webhook>().eq(Webhook::getWebhookKey, request.getWebhookKey())
        );
        if (existing != null) {
            throw new BusinessException(ErrorCode.WEBHOOK_KEY_DUPLICATE);
        }

        Webhook webhook = new Webhook();
        webhook.setWebhookKey(request.getWebhookKey());
        webhook.setName(request.getName());
        webhook.setUrl(request.getUrl());
        webhook.setMethod(request.getMethod() != null ? request.getMethod() : "POST");
        webhook.setHeaders(request.getHeaders() != null ? JsonUtils.toJson(request.getHeaders()) : null);
        webhook.setPayloadTemplate(request.getPayloadTemplate());
        webhook.setTimeout(request.getTimeout() != null ? request.getTimeout() : 5000);
        webhook.setRetryCount(request.getRetryCount() != null ? request.getRetryCount() : 3);
        webhook.setTriggerEvents(request.getTriggerEvents() != null ? JsonUtils.toJson(request.getTriggerEvents()) : null);
        webhook.setProcessKey(request.getProcessKey());
        webhook.setNodeId(request.getNodeId());
        webhook.setStatus(1);
        webhook.setCreateTime(LocalDateTime.now());
        webhook.setUpdateTime(LocalDateTime.now());

        webhookMapper.insert(webhook);
        log.info("创建Webhook配置: key={}", webhook.getWebhookKey());
        return toResponse(webhook);
    }

    /**
     * 获取Webhook配置
     */
    public WebhookResponse getWebhook(String webhookKey) {
        Webhook webhook = webhookMapper.selectOne(
                new LambdaQueryWrapper<Webhook>().eq(Webhook::getWebhookKey, webhookKey)
        );
        if (webhook == null) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_FOUND);
        }
        return toResponse(webhook);
    }

    /**
     * 根据ID获取Webhook
     */
    public Webhook getWebhookById(Long id) {
        return webhookMapper.selectById(id);
    }

    /**
     * 获取Webhook列表
     */
    public List<WebhookResponse> listWebhooks(String processKey) {
        LambdaQueryWrapper<Webhook> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processKey)) {
            wrapper.eq(Webhook::getProcessKey, processKey);
        }
        wrapper.orderByDesc(Webhook::getCreateTime);
        return webhookMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 更新Webhook配置
     */
    public WebhookResponse updateWebhook(String webhookKey, WebhookRequest request) {
        Webhook webhook = webhookMapper.selectOne(
                new LambdaQueryWrapper<Webhook>().eq(Webhook::getWebhookKey, webhookKey)
        );
        if (webhook == null) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_FOUND);
        }

        if (request.getUrl() != null) webhook.setUrl(request.getUrl());
        if (request.getMethod() != null) webhook.setMethod(request.getMethod());
        if (request.getHeaders() != null) webhook.setHeaders(JsonUtils.toJson(request.getHeaders()));
        if (request.getPayloadTemplate() != null) webhook.setPayloadTemplate(request.getPayloadTemplate());
        if (request.getTimeout() != null) webhook.setTimeout(request.getTimeout());
        if (request.getRetryCount() != null) webhook.setRetryCount(request.getRetryCount());
        if (request.getTriggerEvents() != null) webhook.setTriggerEvents(com.flow.engine.common.utils.JsonUtils.toJson(request.getTriggerEvents()));
        if (request.getNodeId() != null) webhook.setNodeId(request.getNodeId());
        webhook.setUpdateTime(LocalDateTime.now());

        webhookMapper.updateById(webhook);
        log.info("更新Webhook配置: key={}", webhookKey);
        return toResponse(webhook);
    }

    /**
     * 删除Webhook配置
     */
    public void deleteWebhook(String webhookKey) {
        Webhook webhook = webhookMapper.selectOne(
                new LambdaQueryWrapper<Webhook>().eq(Webhook::getWebhookKey, webhookKey)
        );
        if (webhook == null) {
            throw new BusinessException(ErrorCode.WEBHOOK_NOT_FOUND);
        }
        webhookMapper.deleteById(webhook.getId());
        log.info("删除Webhook配置: key={}", webhookKey);
    }

    /**
     * 根据事件类型和流程Key查找匹配的Webhook
     */
    public List<Webhook> findWebhooksByEvent(String processKey, String nodeId, String eventType) {
        LambdaQueryWrapper<Webhook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Webhook::getStatus, 1);
        if (StringUtils.hasText(processKey)) {
            wrapper.eq(Webhook::getProcessKey, processKey);
        }
        if (StringUtils.hasText(nodeId)) {
            wrapper.eq(Webhook::getNodeId, nodeId);
        }
        List<Webhook> webhooks = webhookMapper.selectList(wrapper);
        // 过滤包含该事件类型的webhook
        return webhooks.stream()
                .filter(w -> {
                    if (w.getTriggerEvents() == null) return true;
                    try {
                        List<String> events = JsonUtils.fromJson(w.getTriggerEvents(), new TypeReference<List<String>>() {});
                        return events.contains(eventType);
                    } catch (Exception e) {
                        return true;
                    }
                })
                .collect(Collectors.toList());
    }

    private WebhookResponse toResponse(Webhook webhook) {
        WebhookResponse response = new WebhookResponse();
        response.setId(webhook.getId());
        response.setWebhookKey(webhook.getWebhookKey());
        response.setName(webhook.getName());
        response.setUrl(webhook.getUrl());
        response.setMethod(webhook.getMethod());
        response.setHeaders(webhook.getHeaders() != null ? JsonUtils.fromJson(webhook.getHeaders(), new TypeReference<Map<String, String>>() {}) : null);
        response.setPayloadTemplate(webhook.getPayloadTemplate());
        response.setTimeout(webhook.getTimeout());
        response.setRetryCount(webhook.getRetryCount());
        response.setTriggerEvents(webhook.getTriggerEvents() != null ? JsonUtils.fromJson(webhook.getTriggerEvents(), new TypeReference<List<String>>() {}) : null);
        response.setProcessKey(webhook.getProcessKey());
        response.setNodeId(webhook.getNodeId());
        response.setStatus(webhook.getStatus());
        response.setCreateTime(webhook.getCreateTime());
        response.setUpdateTime(webhook.getUpdateTime());
        return response;
    }
}
