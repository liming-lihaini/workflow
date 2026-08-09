package com.flow.engine.node.impl;

import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.model.NodeModel;
import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务任务节点处理器（ISSUE-011 / ISSUE-022）。
 * <p>
 * 执行自动化的服务调用，配置来自节点 properties：
 * <ul>
 *   <li>{@code serviceType}：服务类型，支持 {@code http} / {@code script} / {@code bean}（默认 script）</li>
 *   <li>{@code expression}：SpEL/脚本表达式（script 类型）</li>
 *   <li>{@code resultVariable}：结果存储到流程变量的变量名</li>
 * </ul>
 *
 * <p>当 {@code serviceType=http} 时，从流程表单值（流程变量）取参发起 HTTP 请求：
 * <ul>
 *   <li>{@code url}：请求地址，支持 {@code ${var}} 占位（如 {@code http://host/api/${retainNo}}）</li>
 *   <li>{@code method}：GET/POST/PUT/DELETE，默认 POST</li>
 *   <li>{@code headers}：JSON 字符串，如 {@code {"Authorization":"Bearer ${token}"}}，支持 {@code ${var}}</li>
 *   <li>{@code body}：请求体模板，支持 {@code ${var}} 占位，从表单取值</li>
 *   <li>{@code resultVariable}：响应体（字符串）写入该流程变量；未配置则不写</li>
 * </ul>
 */
@Slf4j
@Component
public class ServiceTaskNodeHandler extends AbstractNodeHandler {

    @Autowired(required = false)
    private RestTemplate webhookRestTemplate;

    @Override
    public String getNodeType() {
        return "serviceTask";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[ServiceTask] 流程实例 {} 进入服务任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext context) {
        NodeModel node = context.getCurrentNode();
        Map<String, Object> props = node != null && node.getProperties() != null ? node.getProperties() : Map.of();
        String serviceType = props.getOrDefault("serviceType", "script").toString();

        switch (serviceType) {
            case "http":
                executeHttp(context, props);
                break;
            case "script":
            case "bean":
            default:
                executeScript(context, props);
                break;
        }
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[ServiceTask] 流程实例 {} 离开服务任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    /**
     * HTTP 服务调用：从表单变量取值渲染参数并发起请求。
     */
    private void executeHttp(ExecutionContext context, Map<String, Object> props) {
        String rawUrl = getString(props, "url");
        if (rawUrl == null || rawUrl.isBlank()) {
            log.warn("[ServiceTask][http] 节点 {} 未配置 url，跳过", context.getCurrentNodeId());
            return;
        }

        // 1. URL 模板渲染（支持 ${var} 路径/查询参数）
        String resolvedUrl = context.resolveTemplate(rawUrl);

        // 2. 方法
        String methodStr = getString(props, "method");
        HttpMethod method = HttpMethod.POST;
        if (methodStr != null && !methodStr.isBlank()) {
            try {
                method = HttpMethod.valueOf(methodStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[ServiceTask][http] 非法 method={}，回退 POST", methodStr);
            }
        }

        // 3. 请求头（支持 ${var} 渲染）
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String headersJson = getString(props, "headers");
        if (headersJson != null && !headersJson.isBlank()) {
            try {
                Map<String, String> customHeaders = JsonUtils.fromJson(headersJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                customHeaders.forEach((k, v) -> headers.set(k, context.resolveTemplate(v)));
            } catch (Exception e) {
                log.warn("[ServiceTask][http] 解析 headers 失败: {}", e.getMessage());
            }
        }

        // 4. 请求体（模板渲染，从表单取值）
        String body = null;
        String bodyTemplate = getString(props, "body");
        if (method != HttpMethod.GET && bodyTemplate != null && !bodyTemplate.isBlank()) {
            body = context.resolveTemplate(bodyTemplate);
        }

        // 4.1 payloadTemplate：结构化 JSON 模板，渲染后解析为对象写入流程变量，
        //      供后续节点/表达式使用；例如 {"retainNo":"${formData.retainNo}","status":"销毁审批中"}
        //      当未配置 body 时，payloadTemplate 渲染结果（JSON 字符串）也作为请求体发送。
        Object payloadObject = null;
        String payloadTemplate = getString(props, "payloadTemplate");
        if (payloadTemplate != null && !payloadTemplate.isBlank()) {
            String resolvedPayload = context.resolveTemplate(payloadTemplate);
            try {
                payloadObject = JsonUtils.fromJson(resolvedPayload, Object.class);
            } catch (Exception e) {
                log.warn("[ServiceTask][http] payloadTemplate 解析为 JSON 失败，按字符串处理: {}", e.getMessage());
                payloadObject = resolvedPayload;
            }
            if (payloadObject != null) {
                String payloadVar = getString(props, "payloadVariable");
                if (payloadVar == null || payloadVar.isBlank()) {
                    payloadVar = "_payload";
                }
                context.setVariable(payloadVar, payloadObject);
                log.info("[ServiceTask][http] payloadTemplate 已写入流程变量 {}: {}", payloadVar, payloadObject);
            }
            // 未显式配置 body 时，payloadTemplate 渲染结果作为请求体
            if (body == null && method != HttpMethod.GET) {
                body = resolvedPayload;
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // 5. 发送请求
        try {
            java.net.URI uri = UriComponentsBuilder.fromHttpUrl(buildSafeHttpUrl(resolvedUrl)).build().toUri();
            ResponseEntity<String> response = webhookRestTemplate.exchange(uri, method, entity, String.class);
            String respBody = response.getBody();
            log.info("[ServiceTask][http] 请求成功: url={}, status={}", resolvedUrl, response.getStatusCode().value());

            String resultVar = getString(props, "resultVariable");
            if (resultVar != null && !resultVar.isBlank()) {
                context.setVariable(resultVar, respBody);
            }
        } catch (Exception e) {
            log.error("[ServiceTask][http] 请求失败: url={}, error={}", resolvedUrl, e.getMessage());
            String resultVar = getString(props, "resultVariable");
            if (resultVar != null && !resultVar.isBlank()) {
                context.setVariable(resultVar, "ERROR:" + e.getMessage());
            }
        }
    }

    /**
     * 脚本/SpEL 求值（兼容原有行为）。
     */
    private void executeScript(ExecutionContext context, Map<String, Object> props) {
        String expression = getString(props, "expression");
        if (expression != null && !expression.isBlank()) {
            try {
                Object result = ExpressionUtils.eval(expression, context.getAllVariables());
                String resultVar = getString(props, "resultVariable");
                if (resultVar != null && !resultVar.isBlank()) {
                    context.setVariable(resultVar, result);
                }
                log.info("[ServiceTask][script] 表达式求值完成: result={}", result);
            } catch (Exception e) {
                log.warn("[ServiceTask][script] 表达式求值失败: {}", e.getMessage());
            }
        } else {
            log.info("[ServiceTask] 服务任务执行完成（无 expression）: nodeId={}", context.getCurrentNodeId());
        }
    }

    /**
     * 若 URL 因 ${var} 渲染出非法字符（如中文路径未编码），做一次编码兜底。
     */
    private String buildSafeHttpUrl(String url) {
        try {
            // 尝试直接构造，失败（含空格/未编码中文等）则对路径段编码
            java.net.URI.create(url);
            return url;
        } catch (Exception e) {
            try {
                return UriComponentsBuilder.fromHttpUrl(url).build().toUriString();
            } catch (Exception ex) {
                return url;
            }
        }
    }

    private String getString(Map<String, Object> props, String key) {
        Object v = props.get(key);
        return v == null ? null : v.toString();
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("serviceType", "服务类型：http（发HTTP请求）/ script（SpEL表达式）/ bean，默认 script");
        schema.put("url", "HTTP 请求地址，支持 ${表单变量}，如 http://host/api/${retainNo}");
        schema.put("method", "HTTP 方法：GET/POST/PUT/DELETE，默认 POST");
        schema.put("headers", "请求头 JSON 字符串，支持 ${表单变量}，如 {\"Authorization\":\"Bearer ${token}\"}");
        schema.put("body", "请求体模板（GET 除外），支持 ${表单变量} 从表单取值，如 {\"retainNo\":\"${retainNo}\"}");
        schema.put("payloadTemplate", "结构化 JSON 模板（GET 除外），渲染后解析为对象写入流程变量，如 {\"retainNo\":\"${formData.retainNo}\",\"status\":\"销毁审批中\"}；未配置 body 时同时作为请求体");
        schema.put("payloadVariable", "payloadTemplate 解析结果写入的流程变量名（默认 _payload）");
        schema.put("expression", "SpEL 表达式（script 类型），如 ${amount * 0.1}");
        schema.put("resultVariable", "结果写入的流程变量名（HTTP 类型写入响应体字符串）");
        return schema;
    }
}
