package com.flow.engine.service;

import com.flow.engine.entity.Webhook;
import com.flow.engine.entity.WebhookLog;
import com.flow.engine.mapper.WebhookLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Webhook调度器单元测试（ISSUE-012）
 */
@SpringBootTest
@DisplayName("Webhook调度器测试")
public class WebhookSchedulerTest {

    @Autowired
    private WebhookScheduler webhookScheduler;

    @Autowired
    private WebhookLogMapper webhookLogMapper;

    @Autowired
    private RestTemplate webhookRestTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(webhookRestTemplate);
    }

    @Test
    @DisplayName("触发Webhook成功")
    void testTriggerWebhookSuccess() {
        // Mock HTTP响应
        mockServer.expect(requestTo("http://localhost:8080/callback"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        Webhook webhook = createTestWebhook("test_success_" + System.currentTimeMillis(), "http://localhost:8080/callback", "POST");

        Map<String, Object> payload = new HashMap<>();
        payload.put("processInstanceId", 1L);
        payload.put("event", "TEST");

        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "TEST_EVENT", 1L, payload);

        assertNotNull(log);
        assertEquals(1, log.getStatus()); // 成功
        assertEquals(200, log.getResponseStatus());
        assertNull(log.getErrorMessage());

        mockServer.verify();
    }

    @Test
    @DisplayName("触发Webhook超时后重试成功")
    void testTriggerWebhookTimeoutRetry() {
        String url = "http://localhost:8080/callback-retry-" + System.currentTimeMillis();

        // 第一次请求失败（超时）
        mockServer.expect(requestTo(url))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withException(new java.io.IOException("Connection timed out")));

        // 重试请求成功
        mockServer.expect(requestTo(url))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        Webhook webhook = createTestWebhook("test_retry_" + System.currentTimeMillis(), url, "POST");
        webhook.setRetryCount(1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "RETRY_TEST");

        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "RETRY_EVENT", 1L, payload);

        assertNotNull(log);
        // 重试后应该成功
        assertEquals(1, log.getStatus());
        assertEquals(1, log.getRetryCount());

        mockServer.verify();
    }

    @Test
    @DisplayName("触发Webhook失败达到最大重试次数")
    void testTriggerWebhookMaxRetry() {
        String url = "http://localhost:8080/callback-fail-" + System.currentTimeMillis();

        // 所有请求都失败
        for (int i = 0; i < 4; i++) {
            mockServer.expect(requestTo(url))
                    .andExpect(method(org.springframework.http.HttpMethod.POST))
                    .andRespond(withException(new java.io.IOException("Connection failed")));
        }

        Webhook webhook = createTestWebhook("test_max_retry_" + System.currentTimeMillis(), url, "POST");
        webhook.setRetryCount(3);

        Map<String, Object> payload = new HashMap<>();

        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "FAIL_EVENT", 1L, payload);

        assertNotNull(log);
        assertEquals(0, log.getStatus()); // 失败
        assertTrue(log.getErrorMessage().contains("重试"));

        mockServer.verify();
    }

    @Test
    @DisplayName("手动重试Webhook日志")
    void testRetryWebhook() {
        String url = "http://localhost:8080/callback-manual-retry-" + System.currentTimeMillis();

        // 创建失败的日志
        WebhookLog failedLog = new WebhookLog();
        failedLog.setWebhookKey("test_manual_retry");
        failedLog.setRequestUrl(url);
        failedLog.setRequestMethod("POST");
        failedLog.setRequestBody("{\"test\":\"data\"}");
        failedLog.setStatus(0);
        failedLog.setRetryCount(0);
        failedLog.setTriggerTime(LocalDateTime.now());
        webhookLogMapper.insert(failedLog);

        // Mock重试成功
        mockServer.expect(requestTo(url))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", MediaType.APPLICATION_JSON));

        WebhookLog retriedLog = webhookScheduler.retryWebhook(failedLog.getId());

        assertNotNull(retriedLog);
        assertEquals(1, retriedLog.getStatus()); // 成功
        assertEquals(1, retriedLog.getRetryCount());

        mockServer.verify();
    }

    @Test
    @DisplayName("使用自定义请求头")
    void testCustomHeaders() {
        String url = "http://localhost:8080/callback-headers-" + System.currentTimeMillis();

        mockServer.expect(requestTo(url))
                .andExpect(header("X-Custom-Header", "test-value"))
                .andExpect(header("Authorization", "Bearer token123"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        Webhook webhook = createTestWebhook("test_headers_" + System.currentTimeMillis(), url, "POST");
        webhook.setHeaders("{\"X-Custom-Header\":\"test-value\",\"Authorization\":\"Bearer token123\"}");

        Map<String, Object> payload = new HashMap<>();
        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "HEADER_TEST", 1L, payload);

        assertEquals(1, log.getStatus());
        mockServer.verify();
    }

    @Test
    @DisplayName("使用模板替换")
    void testPayloadTemplate() {
        String url = "http://localhost:8080/callback-template-" + System.currentTimeMillis();

        mockServer.expect(requestTo(url))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"id\":\"123\"")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        Webhook webhook = createTestWebhook("test_template_" + System.currentTimeMillis(), url, "POST");
        webhook.setPayloadTemplate("{\"id\":\"${processInstanceId}\",\"event\":\"${eventName}\"}");

        Map<String, Object> payload = new HashMap<>();
        payload.put("processInstanceId", "123");
        payload.put("eventName", "TEST");

        WebhookLog log = webhookScheduler.triggerWebhook(webhook, "TEMPLATE_TEST", 1L, payload);

        assertEquals(1, log.getStatus());
        mockServer.verify();
    }

    private Webhook createTestWebhook(String key, String url, String method) {
        Webhook webhook = new Webhook();
        webhook.setWebhookKey(key);
        webhook.setUrl(url);
        webhook.setMethod(method);
        webhook.setTimeout(5000);
        webhook.setRetryCount(0);
        return webhook;
    }
}
