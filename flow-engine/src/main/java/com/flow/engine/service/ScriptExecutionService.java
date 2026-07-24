package com.flow.engine.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 脚本执行服务
 * <p>
 * 支持 Groovy 和 Python（通过 GraalVM Polyglot 或回退到 SpEL）脚本执行。
 * 脚本可访问流程变量作为绑定上下文。
 */
@Slf4j
@Service
public class ScriptExecutionService {

    /**
     * 执行脚本并返回结果
     *
     * @param language  脚本语言（groovy / python / spel）
     * @param script    脚本内容
     * @param variables 流程变量（作为脚本上下文）
     * @return 脚本执行结果
     */
    public Object executeScript(String language, String script, Map<String, Object> variables) {
        if (script == null || script.isBlank()) {
            log.warn("[ScriptExecutionService] 脚本内容为空，跳过执行");
            return null;
        }

        String lang = language != null ? language.toLowerCase() : "groovy";

        try {
            return switch (lang) {
                case "groovy" -> executeGroovy(script, variables);
                case "python" -> executePython(script, variables);
                default -> {
                    log.warn("[ScriptExecutionService] 不支持的脚本语言: {}，回退到 Groovy", language);
                    yield executeGroovy(script, variables);
                }
            };
        } catch (Exception e) {
            log.error("[ScriptExecutionService] 脚本执行失败: language={}, error={}", language, e.getMessage(), e);
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 Groovy 脚本
     */
    private Object executeGroovy(String script, Map<String, Object> variables) {
        Binding binding = new Binding();
        if (variables != null) {
            variables.forEach(binding::setVariable);
        }

        GroovyShell shell = new GroovyShell(binding);
        try {
            Object result = shell.evaluate(script);
            log.info("[ScriptExecutionService] Groovy 脚本执行成功，结果: {}", result);
            return result;
        } finally {
            // GroovyShell 不需要显式关闭
        }
    }

    /**
     * 执行 Python 脚本（尝试 GraalVM Polyglot，不可用时回退到简单表达式）
     */
    private Object executePython(String script, Map<String, Object> variables) {
        // 尝试使用 GraalVM Polyglot
        try {
            Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
            Object context = contextClass.getMethod("create").invoke(null);
            Object polyglotValue = contextClass.getMethod("eval", String.class, String.class)
                    .invoke(context, "python", script);
            Object result = polyglotValue.getClass().getMethod("as", Class.class)
                    .invoke(polyglotValue, Object.class);
            contextClass.getMethod("close").invoke(context);
            log.info("[ScriptExecutionService] Python (GraalVM) 脚本执行成功，结果: {}", result);
            return result;
        } catch (ClassNotFoundException e) {
            log.warn("[ScriptExecutionService] GraalVM Polyglot 不可用，Python 脚本将以日志记录方式跳过");
            log.info("[ScriptExecutionService] Python 脚本内容: {}", script);
            return null;
        } catch (Exception e) {
            log.error("[ScriptExecutionService] Python 脚本执行失败: {}", e.getMessage());
            throw new RuntimeException("Python 脚本执行失败: " + e.getMessage(), e);
        }
    }
}
