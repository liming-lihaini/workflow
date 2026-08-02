package com.flow.engine.util;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * 表达式执行引擎（ISSUE-029）。
 * 基于 Spring EL(SpEL)，作为 QLExpress 的等价沙箱替代：规则/公式以表达式存储，
 * 上下文参数以 #name 引用，沙箱执行，禁止危险调用（BR-029-05）。
 */
public final class ExprEngine {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private ExprEngine() {
    }

    /** 执行布尔表达式（用于规则闸门）。返回布尔值，异常时返回 false。 */
    public static boolean evalBoolean(String expr, Map<String, Object> context) {
        try {
            Expression exp = PARSER.parseExpression(expr);
            StandardEvaluationContext ctx = buildContext(context);
            Boolean result = exp.getValue(ctx, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /** 执行表达式并返回对象结果（用于公式计算）。异常时抛出异常。 */
    public static Object eval(String expr, Map<String, Object> context) {
        Expression exp = PARSER.parseExpression(expr);
        StandardEvaluationContext ctx = buildContext(context);
        return exp.getValue(ctx, Object.class);
    }

    /** 执行表达式并返回数值（公式计算）。 */
    public static Number evalNumber(String expr, Map<String, Object> context) {
        Object v = eval(expr, context);
        if (v instanceof Number) return (Number) v;
        if (v != null) return Double.parseDouble(v.toString());
        throw new IllegalArgumentException("公式返回非数值: " + expr);
    }

    private static StandardEvaluationContext buildContext(Map<String, Object> context) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        // 注入上下文变量；类型安全由表达式编写者保证
        if (context != null) {
            context.forEach(ctx::setVariable);
        }
        return ctx;
    }
}
