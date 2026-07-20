package com.flow.engine.common.utils;

import com.flow.engine.common.exception.FlowException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EL 表达式求值工具。
 *
 * <p>支持两种形态：
 * <ul>
 *   <li>纯表达式（Spring EL）：如 {@code amount > 100}、{@code name == '张三'}</li>
 *   <li>模板形态 {@code ${var}}：在文本中引用变量，如 {@code "hello ${name}"}，会被替换为变量值后整体返回</li>
 * </ul>
 *
 * <p>变量以 {@code variables} Map 注入为 SpEL 根对象，并注册 {@link MapPropertyAccessor}
 * 使裸变量名（如 {@code amount}）可直接解析为 Map 的 key（ISSUE-002 验证用例 {@code ${amount > 100}}）。
 */
public final class ExpressionUtils {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private ExpressionUtils() {
    }

    /**
     * 求布尔表达式，用于条件分支判断。如 {@code amount > 100}。
     *
     * @param expression 表达式字符串
     * @param variables  变量上下文
     * @return 表达式布尔结果
     */
    public static boolean evalBoolean(String expression, Map<String, Object> variables) {
        Object value = eval(expression, variables);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new FlowException(1001, "表达式 [" + expression + "] 求值为非布尔类型: " + value);
    }

    /**
     * 求值表达式，返回原生结果。
     *
     * @param expression 表达式字符串
     * @param variables  变量上下文
     * @return 求值结果
     */
    public static Object eval(String expression, Map<String, Object> variables) {
        try {
            StandardEvaluationContext context = buildContext(variables);
            Expression expr = PARSER.parseExpression(expression);
            return expr.getValue(context);
        } catch (FlowException e) {
            throw e;
        } catch (Exception e) {
            throw new FlowException(1001, "EL 表达式求值失败 [" + expression + "]: " + e.getMessage());
        }
    }

    /**
     * 模板求值：将文本中的 {@code ${var}} 占位替换为变量值。
     * 内部 {@code ${...}} 支持完整 EL 表达式，如 {@code ${amount > 100}}。
     *
     * @param template  模板文本
     * @param variables 变量上下文
     * @return 替换后的文本
     */
    public static String evalTemplate(String template, Map<String, Object> variables) {
        if (template == null) {
            return null;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String inner = matcher.group(1);
            Object value = eval(inner, variables);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static StandardEvaluationContext buildContext(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.addPropertyAccessor(new MapPropertyAccessor());
        if (variables != null) {
            context.setRootObject(variables);
            variables.forEach((k, v) -> context.setVariable(k, v));
        }
        return context;
    }

    /**
     * 允许将 Map 的 key 作为属性名访问（如 {@code amount}、{@code name}）。
     */
    static class MapPropertyAccessor implements PropertyAccessor {

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class<?>[]{Map.class};
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target instanceof Map && ((Map<?, ?>) target).containsKey(name);
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) {
            return new TypedValue(((Map<?, ?>) target).get(name));
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return target instanceof Map;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) {
            ((Map<Object, Object>) target).put(name, newValue);
        }
    }
}
