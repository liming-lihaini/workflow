package com.flow.engine.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionUtils EL 表达式求值测试（ISSUE-002 验证用例）。
 */
class ExpressionUtilsTest {

    @Test
    void testCompareExpression() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 200);
        assertTrue(ExpressionUtils.evalBoolean("amount > 100", vars));
        assertFalse(ExpressionUtils.evalBoolean("amount > 300", vars));
    }

    @Test
    void testVariableGet() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "张三");
        assertEquals("张三", ExpressionUtils.eval("name", vars));
        assertEquals("张三", ExpressionUtils.eval("#name", vars));
    }

    @Test
    void testLogicalExpression() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 200);
        vars.put("vip", true);
        assertTrue(ExpressionUtils.evalBoolean("amount > 100 && vip", vars));
        assertFalse(ExpressionUtils.evalBoolean("amount > 300 && vip", vars));
    }

    @Test
    void testTemplateEval() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "张三");
        vars.put("amount", 200);
        assertEquals("hello 张三", ExpressionUtils.evalTemplate("hello ${name}", vars));
        assertEquals("金额:200", ExpressionUtils.evalTemplate("金额:${amount}", vars));
    }

    @Test
    void testEvalFailure() {
        Map<String, Object> vars = new HashMap<>();
        assertThrows(com.flow.engine.common.exception.FlowException.class,
                () -> ExpressionUtils.evalBoolean("amount >", vars));
    }
}
