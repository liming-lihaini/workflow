package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 结束节点处理器（ISSUE-011）
 * <p>
 * 流程结束标记，触发流程完成逻辑。
 */
@Slf4j
@Component
public class EndNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "end";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[EndNode] 流程实例 {} 进入结束节点", context.getProcessInstanceId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 结束节点无业务逻辑，流转由 NodeExecutor 处理
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[EndNode] 流程实例 {} 离开结束节点，流程完成", context.getProcessInstanceId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of();
    }
}
