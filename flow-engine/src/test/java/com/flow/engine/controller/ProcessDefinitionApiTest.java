package com.flow.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.dto.ProcessDefinitionImportRequest;
import com.flow.engine.dto.ProcessDefinitionUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 流程定义 API 集成测试（ISSUE-003 验证用例）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProcessDefinitionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String LEAVE_PROCESS_JSON = """
            {
              "processKey": "leave_request",
              "processName": "请假申请流程",
              "category": "HR",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "apply", "type": "userTask", "name": "提交申请", "assignee": "申请人"},
                {"id": "approve", "type": "userTask", "name": "部门审批", "assignee": "部门经理"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "apply"},
                {"id": "e2", "source": "apply", "target": "approve"},
                {"id": "e3", "source": "approve", "target": "end"}
              ]
            }
            """;

    @Test
    @DisplayName("创建流程定义 - 成功")
    void testCreateProcessDefinition() throws Exception {
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("test_process_1");
        request.setProcessName("测试流程1");
        request.setCategory("TEST");
        request.setProcessJson(LEAVE_PROCESS_JSON);
        request.setCreateBy("admin");

        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.processKey").value("test_process_1"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.status").value(0));
    }

    @Test
    @DisplayName("创建流程定义 - processKey 重复返回 400")
    void testCreateDuplicateProcessKey() throws Exception {
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("duplicate_key");
        request.setProcessName("测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        // 第一次创建
        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 第二次创建（重复）
        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    @DisplayName("获取流程定义列表")
    void testListProcessDefinitions() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("list_test");
        request.setProcessName("列表测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 获取列表
        mockMvc.perform(get("/api/v1/process/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取流程定义详情")
    void testGetProcessDefinition() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("get_test");
        request.setProcessName("详情测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        MvcResult createResult = mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("data").get("id").asLong();

        // 获取详情
        mockMvc.perform(get("/api/v1/process/definitions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.processKey").value("get_test"));
    }

    @Test
    @DisplayName("更新流程定义")
    void testUpdateProcessDefinition() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("update_test");
        request.setProcessName("更新测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        MvcResult createResult = mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("data").get("id").asLong();

        // 更新
        ProcessDefinitionUpdateRequest updateRequest = new ProcessDefinitionUpdateRequest();
        updateRequest.setProcessName("更新后的流程名称");

        mockMvc.perform(put("/api/v1/process/definitions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.processName").value("更新后的流程名称"));
    }

    @Test
    @DisplayName("部署流程定义 - status 变为 1")
    void testDeployProcessDefinition() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("deploy_test");
        request.setProcessName("部署测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        MvcResult createResult = mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("data").get("id").asLong();

        // 部署
        mockMvc.perform(post("/api/v1/process/definitions/" + id + "/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.deploymentId").isNotEmpty());
    }

    @Test
    @DisplayName("删除流程定义")
    void testDeleteProcessDefinition() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("delete_test");
        request.setProcessName("删除测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        MvcResult createResult = mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(response).get("data").get("id").asLong();

        // 删除
        mockMvc.perform(delete("/api/v1/process/definitions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 验证已删除
        mockMvc.perform(get("/api/v1/process/definitions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("导出导入往返一致性")
    void testExportImportRoundTrip() throws Exception {
        // 先创建一个
        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("export_test");
        request.setProcessName("导出测试流程");
        request.setProcessJson(LEAVE_PROCESS_JSON);

        MvcResult createResult = mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        Long originalId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        // 导出
        MvcResult exportResult = mockMvc.perform(get("/api/v1/process/definitions/" + originalId + "/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String exportResponse = exportResult.getResponse().getContentAsString();
        String exportedJson = objectMapper.readTree(exportResponse).get("data").get("processJson").asText();
        String exportedKey = objectMapper.readTree(exportResponse).get("data").get("processKey").asText();
        String exportedName = objectMapper.readTree(exportResponse).get("data").get("processName").asText();

        // 导入（使用新的key避免冲突）
        ProcessDefinitionImportRequest importRequest = new ProcessDefinitionImportRequest();
        importRequest.setProcessKey(exportedKey + "_imported");
        importRequest.setProcessName(exportedName);
        importRequest.setProcessJson(exportedJson);

        MvcResult importResult = mockMvc.perform(post("/api/v1/process/definitions/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String importResponse = importResult.getResponse().getContentAsString();
        String importedJson = objectMapper.readTree(importResponse).get("data").get("processJson").asText();

        // 验证解析后JSON结构一致（避免编码差异）
        var exportedTree = objectMapper.readTree(exportedJson);
        var importedTree = objectMapper.readTree(importedJson);
        assertEquals(exportedTree.get("nodes").size(), importedTree.get("nodes").size());
        assertEquals(exportedTree.get("edges").size(), importedTree.get("edges").size());
        assertEquals(exportedTree.get("processKey").asText(), importedTree.get("processKey").asText());
    }

    @Test
    @DisplayName("缺失 start 节点校验失败")
    void testMissingStartNode() throws Exception {
        String invalidJson = """
                {
                  "processKey": "invalid",
                  "processName": "无效流程",
                  "nodes": [
                    {"id": "task1", "type": "userTask", "name": "任务1"},
                    {"id": "end", "type": "end", "name": "结束"}
                  ],
                  "edges": []
                }
                """;

        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("missing_start");
        request.setProcessName("缺少开始节点");
        request.setProcessJson(invalidJson);

        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @DisplayName("缺失 end 节点校验失败")
    void testMissingEndNode() throws Exception {
        String invalidJson = """
                {
                  "processKey": "invalid",
                  "processName": "无效流程",
                  "nodes": [
                    {"id": "start", "type": "start", "name": "开始"},
                    {"id": "task1", "type": "userTask", "name": "任务1"}
                  ],
                  "edges": []
                }
                """;

        ProcessDefinitionCreateRequest request = new ProcessDefinitionCreateRequest();
        request.setProcessKey("missing_end");
        request.setProcessName("缺少结束节点");
        request.setProcessJson(invalidJson);

        mockMvc.perform(post("/api/v1/process/definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
