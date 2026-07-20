package com.flow.engine.parser;

import com.flow.engine.common.BusinessException;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProcessJsonParser 单元测试（ISSUE-003 验证用例）
 */
class ProcessJsonParserTest {

    private ProcessJsonParser parser;

    @BeforeEach
    void setUp() {
        parser = new ProcessJsonParser();
    }

    /**
     * 请假流程JSON（start -> userTask -> end）
     */
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

    /**
     * 带条件分支的流程JSON
     */
    private static final String CONDITION_PROCESS_JSON = """
            {
              "processKey": "expense_request",
              "processName": "报销申请流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "apply", "type": "userTask", "name": "提交申请"},
                {"id": "gateway1", "type": "exclusiveGateway", "name": "金额判断"},
                {"id": "manager_approve", "type": "userTask", "name": "经理审批"},
                {"id": "director_approve", "type": "userTask", "name": "总监审批"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "apply"},
                {"id": "e2", "source": "apply", "target": "gateway1"},
                {"id": "e3", "source": "gateway1", "target": "manager_approve", "condition": "amount <= 1000"},
                {"id": "e4", "source": "gateway1", "target": "director_approve", "condition": "amount > 1000"},
                {"id": "e5", "source": "manager_approve", "target": "end"},
                {"id": "e6", "source": "director_approve", "target": "end"}
              ]
            }
            """;

    @Test
    @DisplayName("解析请假流程JSON - 节点数和连线正确")
    void testParseLeaveProcess() {
        ProcessModel model = parser.parse(LEAVE_PROCESS_JSON);

        assertNotNull(model);
        assertEquals("leave_request", model.getProcessKey());
        assertEquals("请假申请流程", model.getProcessName());
        assertEquals(4, model.getNodes().size());
        assertEquals(3, model.getEdges().size());

        // 验证节点类型
        assertEquals("start", model.getNodes().get(0).getType());
        assertEquals("userTask", model.getNodes().get(1).getType());
        assertEquals("end", model.getNodes().get(3).getType());
    }

    @Test
    @DisplayName("getNextNode - 简单流程 start -> userTask -> end")
    void testGetNextNodeSimple() {
        ProcessModel model = parser.parse(LEAVE_PROCESS_JSON);

        // start -> apply
        NodeModel next = parser.getNextNode(model, "start", null);
        assertNotNull(next);
        assertEquals("apply", next.getId());

        // apply -> approve
        next = parser.getNextNode(model, "apply", null);
        assertNotNull(next);
        assertEquals("approve", next.getId());

        // approve -> end
        next = parser.getNextNode(model, "approve", null);
        assertNotNull(next);
        assertEquals("end", next.getId());

        // end -> null (流程结束)
        next = parser.getNextNode(model, "end", null);
        assertNull(next);
    }

    @Test
    @DisplayName("getNextNode - 条件分支：金额 <= 1000 走经理审批")
    void testGetNextNodeConditionSmall() {
        ProcessModel model = parser.parse(CONDITION_PROCESS_JSON);
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 500);

        // gateway1 -> manager_approve (amount <= 1000)
        NodeModel next = parser.getNextNode(model, "gateway1", variables);
        assertNotNull(next);
        assertEquals("manager_approve", next.getId());
    }

    @Test
    @DisplayName("getNextNode - 条件分支：金额 > 1000 走总监审批")
    void testGetNextNodeConditionLarge() {
        ProcessModel model = parser.parse(CONDITION_PROCESS_JSON);
        Map<String, Object> variables = new HashMap<>();
        variables.put("amount", 5000);

        // gateway1 -> director_approve (amount > 1000)
        NodeModel next = parser.getNextNode(model, "gateway1", variables);
        assertNotNull(next);
        assertEquals("director_approve", next.getId());
    }

    @Test
    @DisplayName("evaluateCondition - ${days>3} 当 days=5 时为 true")
    void testEvaluateConditionTrue() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 5);

        boolean result = parser.evaluateCondition("days > 3", variables);
        assertTrue(result);
    }

    @Test
    @DisplayName("evaluateCondition - ${days>3} 当 days=2 时为 false")
    void testEvaluateConditionFalse() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("days", 2);

        boolean result = parser.evaluateCondition("days > 3", variables);
        assertFalse(result);
    }

    @Test
    @DisplayName("校验失败 - 缺少开始节点")
    void testValidateMissingStartNode() {
        String json = """
                {
                  "processKey": "test",
                  "processName": "测试",
                  "nodes": [
                    {"id": "task1", "type": "userTask", "name": "任务1"},
                    {"id": "end", "type": "end", "name": "结束"}
                  ],
                  "edges": []
                }
                """;

        BusinessException ex = assertThrows(BusinessException.class, () -> parser.parse(json));
        assertEquals(1004, ex.getCode());
    }

    @Test
    @DisplayName("校验失败 - 缺少结束节点")
    void testValidateMissingEndNode() {
        String json = """
                {
                  "processKey": "test",
                  "processName": "测试",
                  "nodes": [
                    {"id": "start", "type": "start", "name": "开始"},
                    {"id": "task1", "type": "userTask", "name": "任务1"}
                  ],
                  "edges": []
                }
                """;

        BusinessException ex = assertThrows(BusinessException.class, () -> parser.parse(json));
        assertEquals(1005, ex.getCode());
    }

    @Test
    @DisplayName("校验失败 - JSON为空")
    void testValidateEmptyJson() {
        assertThrows(BusinessException.class, () -> parser.parse(null));
        assertThrows(BusinessException.class, () -> parser.parse(""));
    }

    @Test
    @DisplayName("校验失败 - JSON格式错误")
    void testValidateInvalidJson() {
        assertThrows(BusinessException.class, () -> parser.parse("{invalid json}"));
    }
}
