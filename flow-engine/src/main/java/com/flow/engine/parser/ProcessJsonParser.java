package com.flow.engine.parser;

import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.utils.ExpressionUtils;
import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.model.EdgeModel;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程JSON解析器（TRD §4.1.3）
 * <p>
 * 负责：
 * 1. 将 JSON 解析为 ProcessModel
 * 2. 校验流程定义完整性（必须有 start/end 节点）
 * 3. 获取下一节点（支持条件分支）
 * 4. 条件表达式求值
 */
@Component
public class ProcessJsonParser {

    /** 单次节点数上限（PRD §性能约束） */
    private static final int MAX_NODE_COUNT = 500;

    /**
     * 解析流程定义JSON为 ProcessModel
     *
     * @param processJson 流程定义JSON
     * @return 解析后的流程模型
     */
    public ProcessModel parse(String processJson) {
        if (processJson == null || processJson.isBlank()) {
            throw new BusinessException(ErrorCode.PROCESS_JSON_INVALID, "流程定义JSON不能为空");
        }

        ProcessModel model;
        try {
            model = JsonUtils.fromJson(processJson, ProcessModel.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PROCESS_JSON_INVALID, "流程定义JSON解析失败: " + e.getMessage());
        }

        if (model == null) {
            throw new BusinessException(ErrorCode.PROCESS_JSON_INVALID);
        }

        // 校验
        validate(model);

        return model;
    }

    /**
     * 获取下一个节点
     *
     * @param model         流程模型
     * @param currentNodeId 当前节点ID
     * @param variables     流程变量
     * @return 下一个节点，如果没有则返回 null（流程结束）
     */
    public NodeModel getNextNode(ProcessModel model, String currentNodeId, Map<String, Object> variables) {
        if (model == null || currentNodeId == null) {
            return null;
        }

        List<EdgeModel> edges = model.getEdges();
        if (edges == null || edges.isEmpty()) {
            return null;
        }

        // 找出从当前节点出发的所有连线
        List<EdgeModel> outgoingEdges = edges.stream()
                .filter(e -> currentNodeId.equals(e.getSource()))
                .collect(Collectors.toList());

        if (outgoingEdges.isEmpty()) {
            return null;
        }

        // 如果有条件分支，按条件选择
        for (EdgeModel edge : outgoingEdges) {
            if (edge.getCondition() != null && !edge.getCondition().isBlank()) {
                if (evaluateCondition(edge.getCondition(), variables)) {
                    return findNode(model, edge.getTarget());
                }
            }
        }

        // 没有条件或条件都不满足，取第一条无条件连线
        for (EdgeModel edge : outgoingEdges) {
            if (edge.getCondition() == null || edge.getCondition().isBlank()) {
                return findNode(model, edge.getTarget());
            }
        }

        return null;
    }

    /**
     * 判断连线条件
     *
     * @param condition 条件表达式
     * @param variables 流程变量
     * @return 条件是否满足
     */
    public boolean evaluateCondition(String condition, Map<String, Object> variables) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        try {
            return ExpressionUtils.evalBoolean(condition, variables);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "条件表达式求值失败: " + e.getMessage());
        }
    }

    /**
     * 校验流程定义
     */
    private void validate(ProcessModel model) {
        List<NodeModel> nodes = model.getNodes();
        // 允许空节点（表单配置阶段），严格校验在部署时由 deployForStrict 触发
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // 节点数上限校验
        if (nodes.size() > MAX_NODE_COUNT) {
            throw new BusinessException(ErrorCode.PROCESS_NODE_COUNT_EXCEED,
                    "节点数量 " + nodes.size() + " 超过上限 " + MAX_NODE_COUNT);
        }

        // 必须有开始节点
        boolean hasStart = nodes.stream().anyMatch(n -> "start".equals(n.getType()));
        if (!hasStart) {
            throw new BusinessException(ErrorCode.PROCESS_MISSING_START_NODE);
        }

        // 必须有结束节点
        boolean hasEnd = nodes.stream().anyMatch(n -> "end".equals(n.getType()));
        if (!hasEnd) {
            throw new BusinessException(ErrorCode.PROCESS_MISSING_END_NODE);
        }
    }

    /**
     * 严格解析（部署时使用），必须有完整节点结构
     */
    public ProcessModel parseForDeploy(String processJson) {
        ProcessModel model = parse(processJson);
        if (model.getNodes() == null || model.getNodes().isEmpty()) {
            throw new BusinessException(ErrorCode.PROCESS_JSON_INVALID, "流程定义节点列表不能为空，请先设计流程图再部署");
        }
        return model;
    }

    private NodeModel findNode(ProcessModel model, String nodeId) {
        if (model.getNodes() == null || nodeId == null) {
            return null;
        }
        return model.getNodes().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }
}
