package com.flow.engine.node.impl;

import com.flow.engine.event.NodeEnteredEvent;
import com.flow.engine.node.ExecutionContext;
import com.flow.engine.node.NodeHandler;
import com.flow.engine.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户任务节点处理器（ISSUE-005）
 * <p>
 * 监听 NodeEnteredEvent，当 nodeType="userTask" 时自动创建任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTaskNodeHandler implements NodeHandler {

    private final TaskService taskService;

    @Override
    public String getNodeType() {
        return "userTask";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        // 任务创建由 @EventListener 处理，避免重复
    }

    @Override
    public void execute(ExecutionContext context) {
        // userTask 等待外部完成，此处无逻辑
    }

    @Override
    public void onLeave(ExecutionContext context) {
        // 清理逻辑（暂无）
    }

    /**
     * 监听节点进入事件，为 userTask 自动创建任务
     */
    @EventListener
    public void onNodeEntered(NodeEnteredEvent event) {
        if (!"userTask".equals(event.getNodeType())) {
            return;
        }
        if (event.getProcessInstanceId() == null) {
            log.warn("[UserTaskNodeHandler] processInstanceId 为空，跳过任务创建");
            return;
        }

        log.info("[UserTaskNodeHandler] userTask 节点进入: instanceId={}, nodeId={}, assignee={}, candidates={}",
                event.getProcessInstanceId(), event.getNodeId(),
                event.getAssignee(), event.getCandidateUsers());

        taskService.createTask(
                event.getProcessInstanceId(),
                event.getProcessKey(),
                event.getNodeId(),
                event.getNodeName(),
                event.getAssignee(),
                event.getCandidateUsers()
        );
    }
}
