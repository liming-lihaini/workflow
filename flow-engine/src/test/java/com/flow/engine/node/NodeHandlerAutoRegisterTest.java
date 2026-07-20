package com.flow.engine.node;

import com.flow.engine.node.DummyNodeHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 NodeHandlerAutoConfiguration 在 Spring 上下文启动时自动注册所有 NodeHandler Bean。
 * 对应 ISSUE-002 验证用例：Spring 启动后 registry.getHandler("dummy") 能返回实例。
 */
@SpringBootTest
class NodeHandlerAutoRegisterTest {

    @Autowired
    private NodeHandlerRegistry registry;

    @Test
    void testDummyAutoRegistered() {
        NodeHandler handler = registry.getHandler("dummy");
        assertNotNull(handler);
        assertTrue(handler instanceof DummyNodeHandler);
    }
}
