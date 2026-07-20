package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 排他网关节点处理器（ISSUE-011）
 * <p>
 * 根据条件表达式选择一条分支通过。
 * 条件匹配逻辑由 NodeExecutor.handleExclusiveGateway 处理。
 */
@Slf4j
@Component
public class ExclusiveGatewayNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "exclusiveGateway";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[ExclusiveGateway] 流程实例 {} 进入排他网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 排他网关的条件分支选择由 NodeExecutor 处理
        log.info("[ExclusiveGateway] 排他网关执行: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[ExclusiveGateway] 流程实例 {} 离开排他网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "conditions", "出边条件表达式列表（SpEL），如 [{edge: 'flow1', condition: 'amount > 1000'}]"
        );
    }
}
