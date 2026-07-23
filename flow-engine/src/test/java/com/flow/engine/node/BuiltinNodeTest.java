package com.flow.engine.node;

import com.flow.engine.node.impl.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内置节点处理器测试（ISSUE-011）
 * <p>
 * 验证所有内置节点类型的注册、类型标识和配置Schema。
 */
@DisplayName("内置节点处理器测试")
class BuiltinNodeTest {

    @Test
    @DisplayName("start 节点处理器")
    void testStartNodeHandler() {
        StartNodeHandler handler = new StartNodeHandler();
        assertEquals("start", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("start1");

        // 三阶段不应抛异常
        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        assertNotNull(handler.getConfigSchema());
    }

    @Test
    @DisplayName("end 节点处理器")
    void testEndNodeHandler() {
        EndNodeHandler handler = new EndNodeHandler();
        assertEquals("end", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("end1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));
    }

    @Test
    @DisplayName("serviceTask 节点处理器")
    void testServiceTaskNodeHandler() {
        ServiceTaskNodeHandler handler = new ServiceTaskNodeHandler();
        assertEquals("serviceTask", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("svc1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("expression"));
        assertTrue(schema.containsKey("resultVariable"));
    }

    @Test
    @DisplayName("scriptTask 节点处理器")
    void testScriptTaskNodeHandler() {
        ScriptTaskNodeHandler handler = new ScriptTaskNodeHandler();
        assertEquals("scriptTask", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("script1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("script"));
    }

    @Test
    @DisplayName("exclusiveGateway 节点处理器")
    void testExclusiveGatewayNodeHandler() {
        ExclusiveGatewayNodeHandler handler = new ExclusiveGatewayNodeHandler();
        assertEquals("exclusiveGateway", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("gw1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("conditions"));
    }

    @Test
    @DisplayName("parallelGateway 节点处理器")
    void testParallelGatewayNodeHandler() {
        ParallelGatewayNodeHandler handler = new ParallelGatewayNodeHandler();
        assertEquals("parallelGateway", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("pgw1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("mode"));
    }

    @Test
    @DisplayName("inclusiveGateway 节点处理器")
    void testInclusiveGatewayNodeHandler() {
        InclusiveGatewayNodeHandler handler = new InclusiveGatewayNodeHandler();
        assertEquals("inclusiveGateway", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("igw1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("conditions"));
    }

    @Test
    @DisplayName("subProcess 节点处理器")
    void testSubProcessNodeHandler() {
        SubProcessNodeHandler handler = new SubProcessNodeHandler();
        assertEquals("subProcess", handler.getNodeType());

        ExecutionContext context = new ExecutionContext("1", "def1");
        context.setCurrentNodeId("sub1");

        assertDoesNotThrow(() -> handler.onEnter(context));
        assertDoesNotThrow(() -> handler.execute(context));
        assertDoesNotThrow(() -> handler.onLeave(context));

        Map<String, Object> schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertTrue(schema.containsKey("subProcessKey"));
    }

    @Test
    @DisplayName("userTask 节点处理器")
    void testUserTaskNodeHandler() {
        // userTask 已在 ISSUE-005 实现，此处验证类型标识
        // 由于依赖 TaskService/DeptService/UserMapper/ProcessInstanceMapper，不在此做实例化测试
        assertNotNull(new UserTaskNodeHandler(null, null, null, null).getNodeType());
    }

    @Test
    @DisplayName("所有内置节点类型覆盖 NodeType 枚举")
    void testAllNodeTypesCovered() {
        // 验证所有内置节点类型都有对应的处理器
        String[] expectedTypes = {
                "start", "end", "userTask", "serviceTask", "scriptTask",
                "exclusiveGateway", "parallelGateway", "inclusiveGateway",
                "subProcess"
        };

        NodeHandler[] handlers = {
                new StartNodeHandler(),
                new EndNodeHandler(),
                null, // userTask 需要依赖注入
                new ServiceTaskNodeHandler(),
                new ScriptTaskNodeHandler(),
                new ExclusiveGatewayNodeHandler(),
                new ParallelGatewayNodeHandler(),
                new InclusiveGatewayNodeHandler(),
                new SubProcessNodeHandler()
        };

        for (int i = 0; i < expectedTypes.length; i++) {
            if (handlers[i] != null) {
                assertEquals(expectedTypes[i], handlers[i].getNodeType(),
                        "处理器类型不匹配: " + expectedTypes[i]);
            }
        }
    }
}
