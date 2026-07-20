package com.flow.engine.node;

import com.flow.engine.common.exception.FlowException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点处理器注册表（ISSUE-002）。
 *
 * <p>使用 {@link ConcurrentHashMap} 保证并发安全（TRD §5.2 本地锁+乐观锁场景）。
 * 由 {@link NodeHandlerAutoConfiguration} 在 Spring 容器启动时将所有
 * {@link NodeHandler} Bean 自动注册，引擎按 nodeType 查找。
 */
@Component
public class NodeHandlerRegistry {

    private final Map<String, NodeHandler> registry = new ConcurrentHashMap<>();

    /**
     * 注册节点处理器。
     *
     * @param handler 节点处理器（nodeType 取 {@link NodeHandler#getNodeType()}）
     * @throws FlowException 若 nodeType 重复
     */
    public void register(NodeHandler handler) {
        String nodeType = handler.getNodeType();
        if (nodeType == null || nodeType.isBlank()) {
            throw new FlowException(1001, "节点处理器 [" + handler.getClass().getName() + "] 的 nodeType 不能为空");
        }
        NodeHandler existing = registry.putIfAbsent(nodeType, handler);
        if (existing != null) {
            throw new FlowException(1001, "节点类型 [" + nodeType + "] 重复注册: "
                    + existing.getClass().getName() + " 与 " + handler.getClass().getName());
        }
    }

    /**
     * 按节点类型获取处理器，不存在则抛异常。
     *
     * @param nodeType 节点类型
     * @return 处理器实例
     * @throws FlowException 节点类型未注册
     */
    public NodeHandler getHandler(String nodeType) {
        NodeHandler handler = registry.get(nodeType);
        if (handler == null) {
            throw new FlowException(1001, "节点类型不存在: " + nodeType);
        }
        return handler;
    }

    /**
     * 是否包含指定节点类型。
     */
    public boolean contains(String nodeType) {
        return registry.containsKey(nodeType);
    }

    /**
     * 获取所有已注册的处理器。
     */
    public Collection<NodeHandler> getAllHandlers() {
        return registry.values();
    }

    /**
     * 已注册节点类型数量。
     */
    public int size() {
        return registry.size();
    }
}
