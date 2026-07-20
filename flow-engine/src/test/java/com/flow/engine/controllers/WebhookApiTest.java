package com.flow.engine.controllers;

import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.dto.WebhookRequest;
import com.flow.engine.entity.Webhook;
import com.flow.engine.mapper.WebhookMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Webhook API集成测试（ISSUE-012）
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Webhook API测试")
public class WebhookApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookMapper webhookMapper;

    private String webhookKey;

    @BeforeEach
    void setUp() {
        webhookKey = "api_test_" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("创建Webhook配置")
    void testCreateWebhook() throws Exception {
        WebhookRequest request = new WebhookRequest();
        request.setWebhookKey(webhookKey);
        request.setName("测试Webhook");
        request.setUrl("http://localhost:8080/callback");
        request.setMethod("POST");
        request.setTimeout(5000);
        request.setRetryCount(3);
        request.setTriggerEvents(List.of("PROCESS_STARTED", "PROCESS_COMPLETED"));

        mockMvc.perform(post("/api/v1/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.webhookKey").value(webhookKey))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8080/callback"))
                .andExpect(jsonPath("$.data.method").value("POST"));
    }

    @Test
    @DisplayName("获取Webhook配置")
    void testGetWebhook() throws Exception {
        // 先创建
        createTestWebhook(webhookKey, "http://localhost:8080/get");

        // 再获取
        mockMvc.perform(get("/api/v1/webhooks/" + webhookKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.webhookKey").value(webhookKey));
    }

    @Test
    @DisplayName("获取Webhook列表")
    void testListWebhooks() throws Exception {
        // 创建多个
        createTestWebhook(webhookKey + "_1", "http://localhost:8080/list1");
        createTestWebhook(webhookKey + "_2", "http://localhost:8080/list2");

        mockMvc.perform(get("/api/v1/webhooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("更新Webhook配置")
    void testUpdateWebhook() throws Exception {
        createTestWebhook(webhookKey, "http://localhost:8080/update");

        WebhookRequest updateRequest = new WebhookRequest();
        updateRequest.setUrl("http://localhost:8080/updated");
        updateRequest.setTimeout(10000);

        mockMvc.perform(put("/api/v1/webhooks/" + webhookKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8080/updated"))
                .andExpect(jsonPath("$.data.timeout").value(10000));
    }

    @Test
    @DisplayName("删除Webhook配置")
    void testDeleteWebhook() throws Exception {
        createTestWebhook(webhookKey, "http://localhost:8080/delete");

        mockMvc.perform(delete("/api/v1/webhooks/" + webhookKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证已删除
        mockMvc.perform(get("/api/v1/webhooks/" + webhookKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1024)); // WEBHOOK_NOT_FOUND
    }

    @Test
    @DisplayName("获取回调日志")
    void testGetWebhookLogs() throws Exception {
        // 创建webhook和日志
        Webhook webhook = createTestWebhook(webhookKey, "http://localhost:8080/logs");

        mockMvc.perform(get("/api/v1/webhooks/logs")
                        .param("webhookKey", webhookKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data", isA(List.class)));
    }

    @Test
    @DisplayName("webhookKey重复创建失败")
    void testDuplicateWebhookKey() throws Exception {
        createTestWebhook(webhookKey, "http://localhost:8080/dup1");

        // 再次创建相同key
        WebhookRequest request = new WebhookRequest();
        request.setWebhookKey(webhookKey);
        request.setUrl("http://localhost:8080/dup2");

        mockMvc.perform(post("/api/v1/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1023)); // WEBHOOK_KEY_DUPLICATE
    }

    private Webhook createTestWebhook(String key, String url) {
        Webhook webhook = new Webhook();
        webhook.setWebhookKey(key);
        webhook.setName("测试Webhook");
        webhook.setUrl(url);
        webhook.setMethod("POST");
        webhook.setTimeout(5000);
        webhook.setRetryCount(3);
        webhook.setStatus(1);
        webhook.setCreateTime(LocalDateTime.now());
        webhook.setUpdateTime(LocalDateTime.now());
        webhookMapper.insert(webhook);
        return webhook;
    }
}
