package com.flow.engine.node;

import org.springframework.stereotype.Component;

/**
 * 测试用 Dummy 节点处理器（ISSUE-002 验证用例）。
 *
 * <p>仅用于验证"实现 NodeHandler + @Component 即可被自动注册与发现"的链路，
 * 不做任何业务逻辑。类型标识为 {@code "dummy"}。
 */
@Component
public class DummyNodeHandler extends AbstractNodeHandler {

    @Override
    public String getNodeType() {
        return "dummy";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        context.setLocalVariable("dummyEntered", true);
    }

    @Override
    public void execute(ExecutionContext context) {
        context.setLocalVariable("dummyExecuted", true);
    }

    @Override
    public void onLeave(ExecutionContext context) {
        context.setLocalVariable("dummyLeft", true);
    }
}
