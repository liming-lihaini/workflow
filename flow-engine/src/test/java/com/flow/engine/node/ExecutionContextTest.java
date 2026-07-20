package com.flow.engine.node;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionContext 全局/本地变量作用域与覆盖优先级测试（PRD 变量作用域）。
 */
class ExecutionContextTest {

    @Test
    void testGlobalVariable() {
        ExecutionContext ctx = new ExecutionContext("pi1", "pd1");
        ctx.setVariable("amount", 100);
        assertEquals(100, ctx.getVariable("amount"));
        assertEquals(100, ctx.getVariable("amount", 0));
        assertEquals("default", ctx.getVariable("missing", "default"));
    }

    @Test
    void testLocalOverridesGlobal() {
        ExecutionContext ctx = new ExecutionContext("pi1", "pd1");
        ctx.setVariable("name", "global");
        ctx.setLocalVariable("name", "local");
        // 本地优先
        assertEquals("local", ctx.getVariable("name"));
    }

    @Test
    void testResetLocalScope() {
        ExecutionContext ctx = new ExecutionContext("pi1", "pd1");
        ctx.setVariable("g", 1);
        ctx.setLocalVariable("l", 2);
        ctx.resetLocalScope();
        assertNull(ctx.getLocalVariable("l"));
        assertEquals(1, ctx.getVariable("g"));
    }

    @Test
    void testAllVariablesMerge() {
        ExecutionContext ctx = new ExecutionContext("pi1", "pd1");
        ctx.setVariable("g", 1);
        ctx.setLocalVariable("l", 2);
        var all = ctx.getAllVariables();
        assertEquals(1, all.get("g"));
        assertEquals(2, all.get("l"));
    }

    @Test
    void testEvalFromContext() {
        ExecutionContext ctx = new ExecutionContext("pi1", "pd1");
        ctx.setVariable("amount", 200);
        assertTrue(ctx.evalBoolean("amount > 100"));
    }
}
