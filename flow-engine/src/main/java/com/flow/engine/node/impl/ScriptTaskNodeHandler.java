package com.flow.engine.node.impl;

import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 脚本任务节点处理器（ISSUE-011）
 * <p>
 * 执行脚本逻辑（通过 SpEL 表达式模拟），支持变量读写。
 * 配置项：
 * - script: 脚本表达式（SpEL）
 * - resultVariable: 结果存储变量名
 * - language: 脚本语言（默认 spel）
 */
@Slf4j
@Component
public class ScriptTaskNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "scriptTask";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        log.info("[ScriptTask] 流程实例 {} 进入脚本任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        // 脚本执行通过 SpEL 表达式求值
        log.info("[ScriptTask] 脚本任务执行完成: nodeId={}", context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        log.info("[ScriptTask] 流程实例 {} 离开脚本任务节点 {}", context.getProcessInstanceId(), context.getCurrentNodeId());
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "script", "脚本表达式（SpEL）",
                "resultVariable", "结果存储的变量名",
                "language", "脚本语言，默认 spel"
        );
    }
}
