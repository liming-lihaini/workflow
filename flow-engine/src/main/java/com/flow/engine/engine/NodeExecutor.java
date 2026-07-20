package com.flow.engine.engine;

import com.flow.engine.event.NodeEnteredEvent;
import com.flow.engine.model.EdgeModel;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import com.flow.engine.node.ExecutionContext;
import com.flow.engine.node.NodeHandler;
import com.flow.engine.node.NodeHandlerRegistry;
import com.flow.engine.parser.ProcessJsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 节点执行器（ISSUE-004）
 * <p>
 * 负责调用 NodeHandler 三阶段生命周期，并根据节点类型决定流转策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeExecutor {

    private final NodeHandlerRegistry registry;
    private final ProcessJsonParser jsonParser;
    private final ApplicationEventPublisher eventPublisher;

    /** 自动通过的节点类型（ISSUE-011 全量内置节点） */
    private static final Set<String> AUTO_PASS_TYPES = Set.of(
            "start", "end", "serviceTask", "scriptTask",
            "exclusiveGateway", "parallelGateway", "inclusiveGateway",
            "subProcess"
    );

    /** 需要等待外部触发的节点类型 */
    private static final Set<String> WAIT_TYPES = Set.of("userTask");

    /**
     * 执行节点并返回流转结果
     *
     * @param context    执行上下文
     * @param node       当前节点模型
     * @param model      流程模型
     * @return 流转结果
     */
    public ExecutionResult execute(ExecutionContext context, NodeModel node, ProcessModel model) {
        String nodeType = node.getType();
        log.info("[NodeExecutor] 执行节点: id={}, type={}, name={}", node.getId(), nodeType, node.getName());

        // 发布节点进入事件（携带 assignee/candidateUsers/processKey 供任务创建使用）
        Long instanceId = null;
        try {
            instanceId = Long.parseLong(context.getProcessInstanceId());
        } catch (NumberFormatException ignored) {}
        eventPublisher.publishEvent(new NodeEnteredEvent(
                this, instanceId, node.getId(), nodeType, node.getName(),
                node.getAssignee(), node.getCandidateUsers(),
                model != null ? model.getProcessKey() : null));

        // 查找处理器（如果已注册）
        NodeHandler handler = null;
        if (registry.contains(nodeType)) {
            handler = registry.getHandler(nodeType);
        }

        // 1. onEnter
        if (handler != null) {
            handler.onEnter(context);
        }

        // 2. execute
        if (handler != null) {
            handler.execute(context);
        }

        // 3. onLeave
        if (handler != null) {
            handler.onLeave(context);
        }

        // 根据节点类型决定流转策略
        if ("end".equals(nodeType)) {
            return ExecutionResult.completed();
        }

        if (WAIT_TYPES.contains(nodeType)) {
            // userTask 等需要等待外部触发
            return ExecutionResult.waiting(node.getId());
        }

        if ("exclusiveGateway".equals(nodeType)) {
            // 排他网关：根据条件选择一条分支
            return handleExclusiveGateway(context, node, model);
        }

        if ("parallelGateway".equals(nodeType)) {
            // 并行网关：简化处理，取第一条出边
            return handleParallelGateway(context, node, model);
        }

        if ("inclusiveGateway".equals(nodeType)) {
            // 包容网关：选择满足条件的分支（简化为取第一条匹配分支）
            return handleInclusiveGateway(context, node, model);
        }

        // 默认：顺序流转到下一节点
        NodeModel nextNode = jsonParser.getNextNode(model, node.getId(), context.getAllVariables());
        if (nextNode == null) {
            return ExecutionResult.completed();
        }
        return ExecutionResult.moveTo(nextNode.getId());
    }

    /**
     * 处理排他网关
     */
    private ExecutionResult handleExclusiveGateway(ExecutionContext context, NodeModel node, ProcessModel model) {
        List<EdgeModel> edges = model.getEdges();
        if (edges == null) return ExecutionResult.completed();

        Map<String, Object> variables = context.getAllVariables();

        // 找条件匹配的边
        for (EdgeModel edge : edges) {
            if (node.getId().equals(edge.getSource()) && edge.getCondition() != null && !edge.getCondition().isBlank()) {
                if (jsonParser.evaluateCondition(edge.getCondition(), variables)) {
                    NodeModel target = findNode(model, edge.getTarget());
                    if (target != null) {
                        log.info("[NodeExecutor] 排他网关 {} 条件匹配: {} -> {}", node.getId(), edge.getCondition(), edge.getTarget());
                        return ExecutionResult.moveTo(edge.getTarget());
                    }
                }
            }
        }

        // 没有条件匹配，取默认边（无条件）
        for (EdgeModel edge : edges) {
            if (node.getId().equals(edge.getSource()) && (edge.getCondition() == null || edge.getCondition().isBlank())) {
                return ExecutionResult.moveTo(edge.getTarget());
            }
        }

        return ExecutionResult.completed();
    }

    /**
     * 处理并行网关（简化版：按顺序执行各分支）
     */
    private ExecutionResult handleParallelGateway(ExecutionContext context, NodeModel node, ProcessModel model) {
        List<EdgeModel> edges = model.getEdges();
        if (edges == null) return ExecutionResult.completed();

        // 找出所有出边
        List<EdgeModel> outgoing = edges.stream()
                .filter(e -> node.getId().equals(e.getSource()))
                .toList();

        if (outgoing.isEmpty()) {
            return ExecutionResult.completed();
        }

        // 简化处理：记录并行分支数到变量，取第一条边推进
        // 完整实现需要 fork/join 机制
        context.setVariable("_parallelBranches_" + node.getId(), outgoing.size());
        return ExecutionResult.moveTo(outgoing.get(0).getTarget());
    }

    /**
     * 处理包容网关（ISSUE-011）
     * 简化实现：选择第一条满足条件的分支，与排他网关类似。
     * 完整实现应支持多分支同时激活。
     */
    private ExecutionResult handleInclusiveGateway(ExecutionContext context, NodeModel node, ProcessModel model) {
        List<EdgeModel> edges = model.getEdges();
        if (edges == null) return ExecutionResult.completed();

        Map<String, Object> variables = context.getAllVariables();

        // 找条件匹配的边
        for (EdgeModel edge : edges) {
            if (node.getId().equals(edge.getSource()) && edge.getCondition() != null && !edge.getCondition().isBlank()) {
                if (jsonParser.evaluateCondition(edge.getCondition(), variables)) {
                    NodeModel target = findNode(model, edge.getTarget());
                    if (target != null) {
                        log.info("[NodeExecutor] 包容网关 {} 条件匹配: {} -> {}", node.getId(), edge.getCondition(), edge.getTarget());
                        return ExecutionResult.moveTo(edge.getTarget());
                    }
                }
            }
        }

        // 没有条件匹配，取默认边（无条件）
        for (EdgeModel edge : edges) {
            if (node.getId().equals(edge.getSource()) && (edge.getCondition() == null || edge.getCondition().isBlank())) {
                return ExecutionResult.moveTo(edge.getTarget());
            }
        }

        return ExecutionResult.completed();
    }

    private NodeModel findNode(ProcessModel model, String nodeId) {
        if (model.getNodes() == null) return null;
        return model.getNodes().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 节点执行结果
     */
    public static class ExecutionResult {
        public enum Type { MOVE_TO, WAITING, COMPLETED }

        private final Type type;
        private final String targetNodeId;

        private ExecutionResult(Type type, String targetNodeId) {
            this.type = type;
            this.targetNodeId = targetNodeId;
        }

        public static ExecutionResult moveTo(String nodeId) {
            return new ExecutionResult(Type.MOVE_TO, nodeId);
        }

        public static ExecutionResult waiting(String nodeId) {
            return new ExecutionResult(Type.WAITING, nodeId);
        }

        public static ExecutionResult completed() {
            return new ExecutionResult(Type.COMPLETED, null);
        }

        public Type getType() { return type; }
        public String getTargetNodeId() { return targetNodeId; }
        public boolean isCompleted() { return type == Type.COMPLETED; }
        public boolean isWaiting() { return type == Type.WAITING; }
        public boolean isMoveTo() { return type == Type.MOVE_TO; }
    }
}
