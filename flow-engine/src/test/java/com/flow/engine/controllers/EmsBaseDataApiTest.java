package com.flow.engine.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 环境监测 LIMS 基础数据 API 自动化测试（ISSUE-022，E2E / MockMvc）
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("环境监测基础数据API自动化测试")
public class EmsBaseDataApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("创建客户 -> 列表 -> 停用 闭环")
    void testCustomerLifecycle() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("custName", "API客户" + System.currentTimeMillis());
        body.put("creditCode", "91110000API");

        String resp = mockMvc.perform(post("/api/v1/ems/base/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readValue(resp, JsonNode.class).get("data").get("id").asLong();
        mockMvc.perform(get("/api/v1/ems/base/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/v1/ems/base/customers/" + id + "/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("创建点位经纬度越界返回错误")
    void testPointLngOutOfRange() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("pointName", "越界点");
        body.put("lng", 999.0);
        mockMvc.perform(post("/api/v1/ems/base/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("集成配置加密存储后明文读取")
    void testIntegrationCfgEncryptE2E() throws Exception {
        String key = "E2E_KEY_" + System.currentTimeMillis();
        Map<String, Object> body = new HashMap<>();
        body.put("cfgKey", key);
        body.put("cfgValue", "secret-e2e");
        body.put("encryptFlag", 1);
        mockMvc.perform(post("/api/v1/ems/base/integration-cfg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/ems/base/integration-cfg/" + key + "/plain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("secret-e2e"));
    }

    @Test
    @DisplayName("共享实体：文件归档 + 预警 + 站内信")
    void testSharedEntities() throws Exception {
        Map<String, Object> file = new HashMap<>();
        file.put("bizType", "QUAL");
        file.put("bizId", 1);
        file.put("fileName", "资质.pdf");
        mockMvc.perform(post("/api/v1/ems/shared/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(file)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> alert = new HashMap<>();
        alert.put("alertType", "OVERRUN");
        alert.put("level", "HIGH");
        alert.put("msg", "超标预警");
        mockMvc.perform(post("/api/v1/ems/shared/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alert)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> msg = new HashMap<>();
        msg.put("toUser", "sampler1");
        msg.put("title", "采样通知");
        msg.put("content", "请按时采样");
        mockMvc.perform(post("/api/v1/ems/shared/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
