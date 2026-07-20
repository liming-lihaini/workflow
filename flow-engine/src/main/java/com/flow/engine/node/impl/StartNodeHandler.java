package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 开始节点处理器（ISSUE-011）
 * <p>
 * 流程入口标记，无业务逻辑，自动通过。
 */
@Slf4j
@Component
public class StartNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "start";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[StartNode] 流程实例 {} 进入开始节点", context.getProcessInstanceId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 开始节点无业务逻辑
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[StartNode] 流程实例 {} 离开开始节点", context.getProcessInstanceId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of();
    }
}
