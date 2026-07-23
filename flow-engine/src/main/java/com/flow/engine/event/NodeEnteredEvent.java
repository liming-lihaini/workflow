package com.flow.engine.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 节点进入事件（TRD §5.3）
 */
@Getter
public class NodeEnteredEvent extends ApplicationEvent {

    private final Long processInstanceId;
    private final String nodeId;
    private final String nodeType;
    private final String nodeName;
    /** 处理人（userTask 适用，可为空） */
    private final String assignee;
    /** 候选人（逗号分隔，可为空） */
    private final String candidateUsers;
    /** 流程定义Key */
    private final String processKey;
    /** 处理人分配方式：user-指定用户, dept_leader-部门领导 */
    private final String assigneeType;
    /** 部门ID（assigneeType=dept_leader 时使用） */
    private final Long assigneeDeptId;

    public NodeEnteredEvent(Object source, Long processInstanceId, String nodeId, String nodeType, String nodeName) {
        this(source, processInstanceId, nodeId, nodeType, nodeName, null, null, null, null, null);
    }

    public NodeEnteredEvent(Object source, Long processInstanceId, String nodeId, String nodeType, String nodeName,
                            String assignee, String candidateUsers, String processKey) {
        this(source, processInstanceId, nodeId, nodeType, nodeName, assignee, candidateUsers, processKey, null, null);
    }

    public NodeEnteredEvent(Object source, Long processInstanceId, String nodeId, String nodeType, String nodeName,
                            String assignee, String candidateUsers, String processKey,
                            String assigneeType, Long assigneeDeptId) {
        super(source);
        this.processInstanceId = processInstanceId;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.nodeName = nodeName;
        this.assignee = assignee;
        this.candidateUsers = candidateUsers;
        this.processKey = processKey;
        this.assigneeType = assigneeType;
        this.assigneeDeptId = assigneeDeptId;
    }
}
