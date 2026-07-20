package com.flow.engine.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 节点完成事件（ISSUE-012）
 */
@Getter
public class NodeCompletedEvent extends ApplicationEvent {

    private final Long processInstanceId;
    private final String nodeId;
    private final String nodeType;
    private final String nodeName;
    private final String processKey;

    public NodeCompletedEvent(Object source, Long processInstanceId, String nodeId, String nodeType, String nodeName, String processKey) {
        super(source);
        this.processInstanceId = processInstanceId;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.processKey = processKey;
    }
}
