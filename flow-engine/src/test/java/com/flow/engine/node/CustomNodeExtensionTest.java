package com.flow.engine.node;

import com.flow.engine.node.impl.CustomDemoNodeHandler;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自定义节点扩展测试（ISSUE-011）
 * <p>
 * 验证：
 * 1. CustomDemoNodeHandler 被自动注册到 NodeHandlerRegistry
 * 2. 三阶段生命周期（onEnter/execute/onLeave）均正确执行
 * 3. 执行过程中变量被正确设置
 */
@SpringBootTest
@DisplayName("自定义节点扩展测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomNodeExtensionTest {

    @Autowired
    private NodeHandlerRegistry registry;

    @Test
    @Order(1)
    @DisplayName("自定义节点被自动注册")
    void testCustomNodeAutoRegistered() {
        assertTrue(registry.contains("customDemo"), "customDemo 节点应被自动注册");
        NodeHandler handler = registry.getHandler("customDemo");
        assertNotNull(handler);
        assertInstanceOf(CustomDemoNodeHandler.class, handler);
    }

    @Test
    @Order(2)
    @DisplayName("getHandler('customDemo') 可用")
    void testGetHandlerAvailable() {
        NodeHandler handler = registry.getHandler("customDemo");
        assertEquals("customDemo", handler.getNodeType());
    }

    @Test
    @Order(3)
    @DisplayName("三阶段生命周期均执行")
    void testThreePhaseLifecycle() {
        CustomDemoNodeHandler.resetExecutionLog();

        CustomDemoNodeHandler handler = new CustomDemoNodeHandler();
        ExecutionContext context = new ExecutionContext("test-instance", "test-def");
        context.setCurrentNodeId("custom1");

        // 执行三阶段
        handler.onEnter(context);
        boolean[] log1 = CustomDemoNodeHandler.getExecutionLog();
        assertTrue(log1[0], "onEnter 应已执行");
        assertFalse(log1[1], "execute 尚未执行");

        handler.execute(context);
        boolean[] log2 = CustomDemoNodeHandler.getExecutionLog();
        assertTrue(log2[0], "onEnter 应已执行");
        assertTrue(log2[1], "execute 应已执行");
        assertFalse(log2[2], "onLeave 尚未执行");

        handler.onLeave(context);
        boolean[] log3 = CustomDemoNodeHandler.getExecutionLog();
        assertTrue(log3[0], "onEnter 应已执行");
        assertTrue(log3[1], "execute 应已执行");
        assertTrue(log3[2], "onLeave 应已执行");
    }

    @Test
    @Order(4)
    @DisplayName("onEnter 设置变量")
    void testOnEnterSetsVariables() {
        CustomDemoNodeHandler.resetExecutionLog();
        CustomDemoNodeHandler handler = new CustomDemoNodeHandler();

        ExecutionContext context = new ExecutionContext("test-instance", "test-def");
        context.setCurrentNodeId("custom2");

        handler.onEnter(context);

        assertEquals(true, context.getVariable("_customDemo_entered"));
        assertEquals("custom2", context.getVariable("_customDemo_nodeId"));
    }

    @Test
    @Order(5)
    @DisplayName("execute 设置业务变量")
    void testExecuteSetsVariables() {
        CustomDemoNodeHandler.resetExecutionLog();
        CustomDemoNodeHandler handler = new CustomDemoNodeHandler();

        ExecutionContext context = new ExecutionContext("test-instance", "test-def");
        context.setCurrentNodeId("custom3");

        handler.onEnter(context);
        handler.execute(context);

        assertEquals(true, context.getVariable("_customDemo_executed"));
        assertEquals("custom_result_custom3", context.getVariable("_customDemo_result"));
    }

    @Test
    @Order(6)
    @DisplayName("onLeave 设置完成标记")
    void testOnLeaveSetsVariables() {
        CustomDemoNodeHandler.resetExecutionLog();
        CustomDemoNodeHandler handler = new CustomDemoNodeHandler();

        ExecutionContext context = new ExecutionContext("test-instance", "test-def");
        context.setCurrentNodeId("custom4");

        handler.onEnter(context);
        handler.execute(context);
        handler.onLeave(context);

        assertEquals(true, context.getVariable("_customDemo_completed"));
    }

    @Test
    @Order(7)
    @DisplayName("getConfigSchema 返回配置项")
    void testConfigSchema() {
        CustomDemoNodeHandler handler = new CustomDemoNodeHandler();
        var schema = handler.getConfigSchema();
        assertNotNull(schema);
        assertFalse(schema.isEmpty());
        assertTrue(schema.containsKey("description"));
    }

    @Test
    @Order(8)
    @DisplayName("注册表包含全部内置节点类型")
    void testAllBuiltinTypesRegistered() {
        String[] expectedTypes = {
                "start", "end", "userTask", "serviceTask", "scriptTask",
                "exclusiveGateway", "parallelGateway", "inclusiveGateway",
                "subProcess", "customDemo", "dummy"
        };

        for (String type : expectedTypes) {
            assertTrue(registry.contains(type), "注册表应包含节点类型: " + type);
        }
    }
}
