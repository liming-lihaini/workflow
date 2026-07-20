package com.flow.engine.node;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 节点处理器自动注册配置（ISSUE-002）。
 *
 * <p>Spring 容器启动后，自动收集所有 {@link NodeHandler} Bean 并注册到
 * {@link NodeHandlerRegistry}，实现"声明即注册"的插件式扩展。
 */
@Slf4j
@Configuration
public class NodeHandlerAutoConfiguration {

    private final NodeHandlerRegistry registry;
    private final List<NodeHandler> handlers;

    @Autowired
    public NodeHandlerAutoConfiguration(NodeHandlerRegistry registry, List<NodeHandler> handlers) {
        this.registry = registry;
        this.handlers = handlers;
    }

    @PostConstruct
    public void autoRegister() {
        if (handlers == null) {
            return;
        }
        for (NodeHandler handler : handlers) {
            registry.register(handler);
            log.info("[NodeHandler] 自动注册节点处理器: type={}, class={}",
                    handler.getNodeType(), handler.getClass().getName());
        }
    }
}
