package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 并行网关节点处理器（ISSUE-011）
 * <p>
 * 支持并行分支（fork）和汇聚（join）。
 * - fork：所有出边同时激活
 * - join：等待所有入边完成
 * <p>
 * 当前简化实现：按顺序执行各分支，记录分支数到变量。
 */
@Slf4j
@Component
public class ParallelGatewayNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "parallelGateway";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[ParallelGateway] 流程实例 {} 进入并行网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 并行网关的分支逻辑由 NodeExecutor 处理
        log.info("[ParallelGateway] 并行网关执行: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[ParallelGateway] 流程实例 {} 离开并行网关 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "mode", "网关模式：fork（分叉）/ join（汇聚）",
                "branches", "并行分支数（fork 模式）"
        );
    }
}
