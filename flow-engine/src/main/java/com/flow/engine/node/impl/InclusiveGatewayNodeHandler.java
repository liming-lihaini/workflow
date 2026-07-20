package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 包容网关节点处理器（ISSUE-011）
 * <p>
 * 支持多条件分支：满足条件的分支全部通过（与排他网关不同，可选多条分支）。
 * 至少一条分支必须通过。
 * <p>
 * 条件匹配逻辑由 NodeExecutor 扩展处理。
 */
@Slf4j
@Component
public class InclusiveGatewayNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "inclusiveGateway";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[InclusiveGateway] 流程实例 {} 进入包容网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 包容网关的条件分支选择由 NodeExecutor 处理
        log.info("[InclusiveGateway] 包容网关执行: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[InclusiveGateway] 流程实例 {} 离开包容网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "conditions", "出边条件表达式列表（SpEL），满足条件的分支全部通过",
                "defaultFlow", "默认分支（无条件匹配时使用）"
        );
    }
}
