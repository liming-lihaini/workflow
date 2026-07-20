package com.flow.engine.listener;

import com.flow.engine.entity.Webhook;
import com.flow.engine.event.NodeCompletedEvent;
import com.flow.engine.event.NodeEnteredEvent;
import com.flow.engine.event.ProcessCompletedEvent;
import com.flow.engine.event.ProcessStartedEvent;
import com.flow.engine.service.WebhookScheduler;
import com.flow.engine.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook事件监听器（ISSUE-012）
 * <p>
 * 监听流程事件并触发对应的Webhook回调。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventListener {

    private final WebhookService webhookService;
    private final WebhookScheduler webhookScheduler;

    /**
     * 监听流程启动事件
     */
    @EventListener
    public void onProcessStarted(ProcessStartedEvent event) {
        String eventType = "PROCESS_STARTED";
        log.debug("[WebhookEventListener] 处理事件: {}, processInstanceId={}", eventType, event.getProcessInstanceId());

        List<Webhook> webhooks = webhookService.findWebhooksByEvent(event.getProcessKey(), null, eventType);
        Map<String, Object> payload = buildProcessPayload(event.getProcessInstanceId(), event.getProcessKey(), event.getStartUser());

        for (Webhook webhook : webhooks) {
            webhookScheduler.scheduleAsyncWebhook(webhook, eventType, event.getProcessInstanceId(), payload);
        }
    }

    /**
     * 监听节点进入事件
     */
    @EventListener
    public void onNodeEntered(NodeEnteredEvent event) {
        String eventType = "NODE_ENTERED";
        log.debug("[WebhookEventListener] 处理事件: {}, nodeId={}", eventType, event.getNodeId());

        List<Webhook> webhooks = webhookService.findWebhooksByEvent(event.getProcessKey(), event.getNodeId(), eventType);
        Map<String, Object> payload = buildNodePayload(event.getProcessInstanceId(), event.getNodeId(), event.getNodeType(), event.getNodeName());

        for (Webhook webhook : webhooks) {
            webhookScheduler.scheduleAsyncWebhook(webhook, eventType, event.getProcessInstanceId(), payload);
        }
    }

    /**
     * 监听节点完成事件
     */
    @EventListener
    public void onNodeCompleted(NodeCompletedEvent event) {
        String eventType = "NODE_COMPLETED";
        log.debug("[WebhookEventListener] 处理事件: {}, nodeId={}", eventType, event.getNodeId());

        List<Webhook> webhooks = webhookService.findWebhooksByEvent(event.getProcessKey(), event.getNodeId(), eventType);
        Map<String, Object> payload = buildNodePayload(event.getProcessInstanceId(), event.getNodeId(), event.getNodeType(), event.getNodeName());

        for (Webhook webhook : webhooks) {
            webhookScheduler.scheduleAsyncWebhook(webhook, eventType, event.getProcessInstanceId(), payload);
        }
    }

    /**
     * 监听流程完成事件
     */
    @EventListener
    public void onProcessCompleted(ProcessCompletedEvent event) {
        String eventType = "PROCESS_COMPLETED";
        log.debug("[WebhookEventListener] 处理事件: {}, processInstanceId={}", eventType, event.getProcessInstanceId());

        List<Webhook> webhooks = webhookService.findWebhooksByEvent(event.getProcessKey(), null, eventType);
        Map<String, Object> payload = buildProcessPayload(event.getProcessInstanceId(), event.getProcessKey(), null);

        for (Webhook webhook : webhooks) {
            webhookScheduler.scheduleAsyncWebhook(webhook, eventType, event.getProcessInstanceId(), payload);
        }
    }

    private Map<String, Object> buildProcessPayload(Long processInstanceId, String processKey, String startUser) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("processInstanceId", processInstanceId);
        payload.put("processKey", processKey);
        payload.put("startUser", startUser);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private Map<String, Object> buildNodePayload(Long processInstanceId, String nodeId, String nodeType, String nodeName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("processInstanceId", processInstanceId);
        payload.put("nodeId", nodeId);
        payload.put("nodeType", nodeType);
        payload.put("nodeName", nodeName);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }
}
