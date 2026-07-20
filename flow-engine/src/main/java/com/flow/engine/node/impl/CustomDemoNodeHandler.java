package com.flow.engine.node.impl;

import com.flow.engine.node.AbstractNodeHandler;
import com.flow.engine.node.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自定义节点样例（ISSUE-011）
 * <p>
 * 演示如何通过实现 NodeHandler 接口 + @Component 注解来扩展自定义节点类型。
 * 引擎启动时自动注册，无需修改框架代码。
 * <p>
 * 本样例展示完整的三阶段生命周期：
 * 1. onEnter —— 进入节点时记录日志并初始化变量
 * 2. execute —— 执行核心业务逻辑（修改流程变量）
 * 3. onLeave —— 离开节点时清理并记录日志
 * <p>
 * 使用方式：在流程 JSON 中定义 type="customDemo" 的节点即可被引擎识别和执行。
 */
@Slf4j
@Component
public class CustomDemoNodeHandler extends AbstractNodeHandler {

    /** 记录三阶段执行状态，供测试验证 */
    private static final ThreadLocal<boolean[]> EXECUTION_LOG = ThreadLocal.withInitial(() -> new boolean[3]);

    @Override
    public String getNodeType() {
        return "customDemo";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        EXECUTION_LOG.get()[0] = true;
        log.info("[CustomDemo] onEnter: 流程实例 {} 进入自定义节点 {}",
                context.getProcessInstanceId(), context.getCurrentNodeId());

        // 初始化自定义变量
        context.setVariable("_customDemo_entered", true);
        context.setVariable("_customDemo_nodeId", context.getCurrentNodeId());
    }

    @Override
    public void execute(ExecutionContext context) {
        EXECUTION_LOG.get()[1] = true;
        log.info("[CustomDemo] execute: 自定义节点 {} 执行业务逻辑", context.getCurrentNodeId());

        // 模拟业务逻辑：设置一些变量
        context.setVariable("_customDemo_executed", true);
        context.setVariable("_customDemo_result", "custom_result_" + context.getCurrentNodeId());
    }

    @Override
    public void onLeave(ExecutionContext context) {
        EXECUTION_LOG.get()[2] = true;
        log.info("[CustomDemo] onLeave: 流程实例 {} 离开自定义节点 {}",
                context.getProcessInstanceId(), context.getCurrentNodeId());

        // 清理逻辑
        context.setVariable("_customDemo_completed", true);
    }

    /**
     * 获取三阶段执行日志（供测试使用）
     *
     * @return boolean[3]：[onEnter, execute, onLeave] 是否已执行
     */
    public static boolean[] getExecutionLog() {
        return EXECUTION_LOG.get();
    }

    /**
     * 重置执行日志（供测试使用）
     */
    public static void resetExecutionLog() {
        EXECUTION_LOG.remove();
    }

    @Override
    public Map<String, Object> getConfigSchema() {
        return Map.of(
                "description", "自定义节点样例，演示三阶段生命周期",
                "customParam", "自定义参数（可选）"
        );
    }
}
