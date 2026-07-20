package com.flow.engine.node;

import com.flow.engine.common.exception.FlowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NodeHandlerRegistry 注册/查找/异常断言（ISSUE-002 验证用例）。
 */
class NodeHandlerRegistryTest {

    private NodeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new NodeHandlerRegistry();
    }

    @Test
    void testRegisterAndGet() {
        NodeHandler dummy = new NodeHandler() {
            @Override
            public String getNodeType() {
                return "dummy";
            }

            @Override
            public void onEnter(ExecutionContext context) {
            }

            @Override
            public void execute(ExecutionContext context) {
            }

            @Override
            public void onLeave(ExecutionContext context) {
            }
        };
        registry.register(dummy);
        assertSame(dummy, registry.getHandler("dummy"));
        assertTrue(registry.contains("dummy"));
        assertEquals(1, registry.size());
    }

    @Test
    void testGetNotExistsThrows() {
        assertThrows(FlowException.class, () -> registry.getHandler("not-exist"));
    }

    @Test
    void testDuplicateRegisterThrows() {
        NodeHandler h1 = makeHandler("dup");
        NodeHandler h2 = makeHandler("dup");
        registry.register(h1);
        assertThrows(FlowException.class, () -> registry.register(h2));
    }

    @Test
    void testRegisterEmptyTypeThrows() {
        assertThrows(FlowException.class, () -> registry.register(makeHandler("  ")));
    }

    private NodeHandler makeHandler(String type) {
        return new NodeHandler() {
            @Override
            public String getNodeType() {
                return type;
            }

            @Override
            public void onEnter(ExecutionContext context) {
            }

            @Override
            public void execute(ExecutionContext context) {
            }

            @Override
            public void onLeave(ExecutionContext context) {
            }
        };
    }
}
