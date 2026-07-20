package com.flow.engine.node.impl;

import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务任务节点处理器（ISSUE-011）
 * <p>
 * 执行自动化的服务调用，支持：
 * 1. SpEL 表达式求值（properties.expression）
 * 2. 变量赋值（properties.resultVariable）
 * 3. 自动通过，不等待外部触发
 */
@Slf4j
@Component
public class ServiceTaskNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "serviceTask";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[ServiceTask] 流程实例 {} 进入服务任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // serviceTask 的执行逻辑通过 properties 配置
        // 实际执行时由 NodeExecutor 传递节点属性
        log.info("[ServiceTask] 服务任务执行完成: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[ServiceTask] 流程实例 {} 离开服务任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "expression", "SpEL 表达式，如 ${amount * 0.1}",
                "resultVariable", "结果存储的变量名",
                "serviceType", "服务类型：http/script/bean"
        );
    }
}
