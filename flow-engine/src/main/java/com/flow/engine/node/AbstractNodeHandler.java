package com.flow.engine.node;

/**
 * 节点处理器抽象基类，提供生命周期三阶段的空实现，便于子类只覆盖关心的阶段。
 */
public abstract class AbstractNodeHandler implements NodeHandler {

    @Override
    public void onEnter(ExecutionContext context) {
        // 默认空实现
    }

    @Override
    public void execute(ExecutionContext context) {
        // 默认空实现，子类必须覆盖或以具体节点实现
    }

    @Override
    public void onLeave(ExecutionContext context) {
        // 默认空实现
    }
}
