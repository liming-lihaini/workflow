package com.flow.engine.engine;

import com.flow.engine.common.enums.ProcessStatus;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.dto.ProcessInstanceResponse;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.service.ProcessDefinitionService;
import com.flow.engine.service.ProcessInstanceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流程引擎端到端测试（ISSUE-004）
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlowEngineE2ETest {

    @Autowired
    private ProcessDefinitionService definitionService;

    @Autowired
    private ProcessInstanceService instanceService;

    @Autowired
    private ProcessInstanceMapper instanceMapper;

    /**
     * 简单流程：start -> userTask -> end
     */
    private static final String SIMPLE_PROCESS_JSON = """
            {
              "processKey": "e2e_simple",
              "processName": "E2E简单流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "task1", "type": "userTask", "name": "审批"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "task1"},
                {"id": "e2", "source": "task1", "target": "end"}
              ]
            }
            """;

    /**
     * 条件分支流程
     */
    private static final String CONDITION_PROCESS_JSON = """
            {
              "processKey": "e2e_condition",
              "processName": "E2E条件流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "gateway1", "type": "exclusiveGateway", "name": "金额判断"},
                {"id": "task_small", "type": "userTask", "name": "小额审批"},
                {"id": "task_large", "type": "userTask", "name": "大额审批"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "gateway1"},
                {"id": "e2", "source": "gateway1", "target": "task_small", "condition": "amount <= 1000"},
                {"id": "e3", "source": "gateway1", "target": "task_large", "condition": "amount > 1000"},
                {"id": "e4", "source": "task_small", "target": "end"},
                {"id": "e5", "source": "task_large", "target": "end"}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        // 创建流程定义（忽略重复）
        try {
            ProcessDefinitionCreateRequest req = new ProcessDefinitionCreateRequest();
            req.setProcessKey("e2e_simple");
            req.setProcessName("E2E简单流程");
            req.setProcessJson(SIMPLE_PROCESS_JSON);
            definitionService.create(req);
            // 部署
            var list = definitionService.list("e2e_simple", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
            // 已存在则跳过
        }

        try {
            ProcessDefinitionCreateRequest req = new ProcessDefinitionCreateRequest();
            req.setProcessKey("e2e_condition");
            req.setProcessName("E2E条件流程");
            req.setProcessJson(CONDITION_PROCESS_JSON);
            definitionService.create(req);
            var list = definitionService.list("e2e_condition", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("发起实例：status=运行中")
    @Order(1)
    void testStartProcess() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("e2e_simple");
        request.setBusinessKey("BIZ001");
        request.setStartUser("testUser");

        ProcessInstanceResponse response = instanceService.start(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(ProcessStatus.RUNNING.getValue(), response.getStatus());
        assertEquals("e2e_simple", response.getProcessKey());
    }

    @Test
    @DisplayName("顺序流转：start->userTask->end 跑通")
    @Order(2)
    void testSequentialFlow() {
        // 发起
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("e2e_simple");
        request.setStartUser("testUser");

        ProcessInstanceResponse response = instanceService.start(request);
        Long instanceId = response.getId();

        // 此时应该在 userTask 等待
        ProcessInstance instance = instanceMapper.selectById(instanceId);
        assertEquals(ProcessStatus.RUNNING.getValue(), instance.getStatus());
        assertEquals("task1", instance.getCurrentNodeId());

        // 完成任务 -> 流转到 end -> 自动完成
        instanceService.completeTask(instanceId, null);

        // 验证流程完成
        instance = instanceMapper.selectById(instanceId);
        assertEquals(ProcessStatus.COMPLETED.getValue(), instance.getStatus());
        assertNotNull(instance.getEndTime());
    }

    @Test
    @DisplayName("条件分支：amount<=1000 走小额审批")
    @Order(3)
    void testConditionBranchSmall() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("e2e_condition");
        request.setStartUser("testUser");
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 500);
        request.setVariables(vars);

        ProcessInstanceResponse response = instanceService.start(request);
        Long instanceId = response.getId();

        ProcessInstance instance = instanceMapper.selectById(instanceId);
        // 应该走到小额审批
        assertEquals("task_small", instance.getCurrentNodeId());
    }

    @Test
    @DisplayName("条件分支：amount>1000 走大额审批")
    @Order(4)
    void testConditionBranchLarge() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("e2e_condition");
        request.setStartUser("testUser");
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 5000);
        request.setVariables(vars);

        ProcessInstanceResponse response = instanceService.start(request);
        Long instanceId = response.getId();

        ProcessInstance instance = instanceMapper.selectById(instanceId);
        // 应该走到大额审批
        assertEquals("task_large", instance.getCurrentNodeId());
    }

    @Test
    @DisplayName("变量：发起传入变量，可查询")
    @Order(5)
    void testVariables() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("e2e_simple");
        request.setStartUser("testUser");
        Map<String, Object> vars = new HashMap<>();
        vars.put("days", 5);
        vars.put("reason", "vacation");
        request.setVariables(vars);

        ProcessInstanceResponse response = instanceService.start(request);
        Long instanceId = response.getId();

        Map<String, Object> retrieved = instanceService.getVariables(instanceId);
        assertEquals(5, retrieved.get("days"));
        assertEquals("vacation", retrieved.get("reason"));
    }
}
