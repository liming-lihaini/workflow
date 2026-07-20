package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 子流程节点处理器（ISSUE-011）
 * <p>
 * 嵌套子流程：进入时启动子流程实例，子流程完成后回到主流程继续。
 * <p>
 * 配置项：
 * - subProcessKey: 子流程定义Key
 * - inheritVariables: 是否继承主流程变量
 */
@Slf4j
@Component
public class SubProcessNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "subProcess";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[SubProcess] 流程实例 {} 进入子流程节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 子流程的实际执行需要调用 FlowEngine 启动子流程实例
        // 当前简化实现：记录日志，等待外部触发
        log.info("[SubProcess] 子流程执行: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[SubProcess] 流程实例 {} 离开子流程节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "subProcessKey", "子流程定义Key",
                "inheritVariables", "是否继承主流程变量（true/false）",
                "waitForCompletion", "是否等待子流程完成（true/false）"
        );
    }
}
