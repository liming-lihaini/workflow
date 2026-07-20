package com.flow.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.service.ProcessDefinitionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 任务 API 集成测试（ISSUE-005，MockMvc）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProcessDefinitionService definitionService;

    private static final String PROCESS_JSON = """
            {
              "processKey": "task_api_test",
              "processName": "任务API测试流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "task1", "type": "userTask", "name": "审批", "assignee": "apiUser1", "candidateUsers": "apiUser1,apiUser2"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "task1"},
                {"id": "e2", "source": "task1", "target": "end"}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        try {
            ProcessDefinitionCreateRequest req = new ProcessDefinitionCreateRequest();
            req.setProcessKey("task_api_test");
            req.setProcessName("任务API测试流程");
            req.setProcessJson(PROCESS_JSON);
            definitionService.create(req);
            var list = definitionService.list("task_api_test", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("全链路：发起->待办->签收->完成->已办")
    @Order(1)
    void testFullChain() throws Exception {
        // 1. 发起流程
        String startBody = """
                {"processKey":"task_api_test","startUser":"starter"}
                """;
        MvcResult startResult = mockMvc.perform(post("/api/v1/process/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(startBody))
                .andExpect(status().isOk())
                .andReturn();

        // 2. 查待办
        MvcResult todoResult = mockMvc.perform(get("/api/v1/tasks/todo")
                        .param("userId", "apiUser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].nodeId").value("task1"))
                .andReturn();

        // 提取任务ID
        String todoJson = todoResult.getResponse().getContentAsString();
        var todoMap = objectMapper.readValue(todoJson, Map.class);
        var dataList = (java.util.List<java.util.Map<String, Object>>) todoMap.get("data");
        Number taskIdNum = (Number) dataList.get(0).get("id");
        Long taskId = taskIdNum.longValue();

        // 3. 签收
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"apiUser1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1));

        // 4. 完成
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"apiUser1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2));

        // 5. 查已办
        mockMvc.perform(get("/api/v1/tasks/done")
                        .param("userId", "apiUser1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(taskId));
    }

    @Test
    @DisplayName("任务详情：GET /api/v1/tasks/{id}")
    @Order(2)
    void testGetById() throws Exception {
        // 发起流程
        mockMvc.perform(post("/api/v1/process/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processKey":"task_api_test","startUser":"starter"}
                                """))
                .andExpect(status().isOk());

        // 获取待办
        MvcResult todoResult = mockMvc.perform(get("/api/v1/tasks/todo")
                        .param("userId", "apiUser1"))
                .andExpect(status().isOk())
                .andReturn();

        String todoJson = todoResult.getResponse().getContentAsString();
        var todoMap = objectMapper.readValue(todoJson, Map.class);
        var dataList = (java.util.List<java.util.Map<String, Object>>) todoMap.get("data");
        if (!dataList.isEmpty()) {
            Number taskIdNum = (Number) dataList.get(0).get("id");
            mockMvc.perform(get("/api/v1/tasks/" + taskIdNum.longValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(taskIdNum.longValue()));
        }
    }

    @Test
    @DisplayName("转办：POST /api/v1/tasks/{id}/transfer")
    @Order(3)
    void testTransfer() throws Exception {
        // 发起流程
        mockMvc.perform(post("/api/v1/process/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"processKey":"task_api_test","startUser":"starter"}
                                """))
                .andExpect(status().isOk());

        MvcResult todoResult = mockMvc.perform(get("/api/v1/tasks/todo")
                        .param("userId", "apiUser1"))
                .andExpect(status().isOk())
                .andReturn();

        String todoJson = todoResult.getResponse().getContentAsString();
        var todoMap = objectMapper.readValue(todoJson, Map.class);
        var dataList = (java.util.List<java.util.Map<String, Object>>) todoMap.get("data");
        if (!dataList.isEmpty()) {
            Number taskIdNum = (Number) dataList.get(0).get("id");
            Long taskId = taskIdNum.longValue();

            mockMvc.perform(post("/api/v1/tasks/" + taskId + "/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"operatorId":"apiUser1","targetUserId":"apiUser3"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignee").value("apiUser3"));
        }
    }
}
