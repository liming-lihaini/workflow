package com.flow.engine.node;

import java.util.Map;

/**
 * 节点处理器接口（插件式节点框架核心扩展点，ISSUE-002）。
 *
 * <p>新增节点只需实现本接口并声明为 Spring {@code @Component}，即会被
 * {@link NodeHandlerAutoConfiguration} 自动注册到 {@link NodeHandlerRegistry}，
 * 引擎（ISSUE-004）通过 {@code getNodeType()} 查找并执行，无需改动框架代码。
 *
 * <p>生命周期三阶段（PRD §自定义节点扩展）：
 * <ol>
 *   <li>{@link #onEnter} —— 节点进入</li>
 *   <li>{@link #execute} —— 节点执行</li>
 *   <li>{@link #onLeave} —— 节点离开</li>
 * </ol>
 */
public interface NodeHandler {

    /**
     * 节点类型标识（全局唯一），引擎据此查找处理器。
     *
     * @return 节点类型，如 "start"、"userTask"、"dummy"
     */
    String getNodeType();

    /**
     * 节点进入时回调。可在此做前置校验、初始化。
     */
    void onEnter(ExecutionContext context);

    /**
     * 节点执行逻辑。核心业务处理。
     */
    void execute(ExecutionContext context);

    /**
     * 节点离开时回调。可在此做清理、触发后续节点。
     */
    void onLeave(ExecutionContext context);

    /**
     * 返回该节点的可配置项 Schema（供 ISSUE-019 设计器渲染）。
     * 默认返回空 Map，表示无需配置。
     *
     * @return 配置项结构（key -> 描述/类型约束）
     */
    default Map<String, Object> getConfigSchema() {
        return Map.of();
    }
}
