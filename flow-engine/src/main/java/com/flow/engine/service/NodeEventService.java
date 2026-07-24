package com.flow.engine.service;

import com.flow.engine.model.NodeModel;
import com.flow.engine.node.ExecutionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 节点事件服务
 * <p>
 * 解析节点 JSON 中的 events 配置，在节点生命周期触发点执行对应脚本。
 * 事件类型：beforeEnter / afterEnter / afterComplete / afterReject
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeEventService {

    private final ScriptExecutionService scriptExecutionService;

    /**
     * 触发节点事件
     * <p>
     * 遍历节点的 events 数组，匹配 eventType，调用 ScriptExecutionService 执行脚本。
     *
     * @param eventType 事件类型（beforeEnter/afterEnter/afterComplete/afterReject）
     * @param node      节点模型
     * @param context   执行上下文
     */
    public void fireEvent(String eventType, NodeModel node, ExecutionContext context) {
        if (node == null || node.getEvents() == null || node.getEvents().isEmpty()) {
            return;
        }

        List<Map<String, Object>> events = node.getEvents();
        for (Map<String, Object> event : events) {
            String type = getStringValue(event, "eventType");
            if (!eventType.equals(type)) {
                continue;
            }

            String language = getStringValue(event, "language");
            String script = getStringValue(event, "script");

            if (script == null || script.isBlank()) {
                log.warn("[NodeEventService] 节点 {} 事件 {} 脚本内容为空，跳过", node.getId(), eventType);
                continue;
            }

            log.info("[NodeEventService] 触发节点事件: nodeId={}, eventType={}, language={}",
                    node.getId(), eventType, language);

            try {
                Map<String, Object> variables = context.getAllVariables();
                // 添加节点元信息到脚本上下文
                variables.put("_nodeId", node.getId());
                variables.put("_nodeType", node.getType());
                variables.put("_nodeName", node.getName());
                variables.put("_eventType", eventType);
                variables.put("_processInstanceId", context.getProcessInstanceId());

                Object result = scriptExecutionService.executeScript(language, script, variables);
                log.info("[NodeEventService] 事件脚本执行完成: nodeId={}, eventType={}, result={}",
                        node.getId(), eventType, result);

                // 如果脚本返回了 Map，将结果合并到流程变量
                if (result instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    resultMap.forEach(context::setVariable);
                }
            } catch (Exception e) {
                log.error("[NodeEventService] 事件脚本执行失败: nodeId={}, eventType={}, error={}",
                        node.getId(), eventType, e.getMessage(), e);
                // 不中断流程执行，仅记录错误
            }
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
