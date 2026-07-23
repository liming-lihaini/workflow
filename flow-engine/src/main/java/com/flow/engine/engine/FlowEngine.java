package com.flow.engine.engine;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.ProcessStatus;
import com.flow.engine.dto.ProcessDefinitionResponse;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.event.ProcessCompletedEvent;
import com.flow.engine.event.ProcessStartedEvent;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import com.flow.engine.node.ExecutionContext;
import com.flow.engine.parser.ProcessJsonParser;
import com.flow.engine.service.ProcessDefinitionService;
import com.flow.engine.service.VariableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程运行引擎核心（ISSUE-004，TRD §4.1.1）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowEngine {

    private final ProcessDefinitionService definitionService;
    private final ProcessInstanceMapper instanceMapper;
    private final ProcessJsonParser jsonParser;
    private final NodeExecutor nodeExecutor;
    private final VariableService variableService;
    private final ApplicationEventPublisher eventPublisher;

    /** 最大节点执行步数，防止死循环 */
    private static final int MAX_STEPS = 100;

    /**
     * 发起流程实例
     *
     * @param processKey  流程定义Key
     * @param businessKey 业务主键
     * @param startUser   发起人
     * @param variables   流程变量
     * @return 流程实例
     */
    @Transactional
    public ProcessInstance startProcess(String processKey, String businessKey, String startUser, Map<String, Object> variables) {
        // 1. 查找已部署的流程定义
        ProcessDefinitionResponse defResp = definitionService.getByKey(processKey);
        if (defResp.getStatus() == null || defResp.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PROCESS_NOT_DEPLOYED, "流程定义 '" + processKey + "' 未部署");
        }

        // 2. 解析流程JSON
        ProcessModel model = jsonParser.parse(defResp.getProcessJson());

        // 3. 创建流程实例
        ProcessInstance instance = new ProcessInstance();
        instance.setProcessKey(processKey);
        instance.setProcessName(defResp.getProcessName());
        instance.setProcessVersion(defResp.getVersion());
        instance.setBusinessKey(businessKey);
        instance.setStatus(ProcessStatus.RUNNING.getValue());
        instance.setStartUser(startUser);
        instance.setStartTime(LocalDateTime.now());
        instance.setVersion(0);
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());

        instanceMapper.insert(instance);

        // 4. 保存流程变量
        if (variables != null) {
            variableService.saveVariables(instance.getId(), variables);
        }

        // 5. 发布流程启动事件
        eventPublisher.publishEvent(new ProcessStartedEvent(this, instance.getId(), processKey, startUser));

        // 6. 从 start 节点开始执行
        ExecutionContext context = buildContext(instance, variables);
        executeFromStart(context, model);

        return instanceMapper.selectById(instance.getId());
    }

    /**
     * 完成任务并推进流程
     *
     * @param instanceId 流程实例ID
     * @param variables  任务变量
     */
    @Transactional
    public void completeTask(Long instanceId, Map<String, Object> variables) {
        ProcessInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND);
        }
        if (instance.getStatus() != ProcessStatus.RUNNING.getValue()) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "流程实例不在运行状态");
        }

        // 合并变量
        if (variables != null) {
            variableService.updateVariables(instanceId, variables);
        }

        // 获取当前变量
        Map<String, Object> allVars = variableService.getVariables(instanceId);

        // 解析流程定义
        ProcessDefinitionResponse defResp = definitionService.getByKey(instance.getProcessKey());
        ProcessModel model = jsonParser.parse(defResp.getProcessJson());

        // 构建上下文
        ExecutionContext context = buildContext(instance, allVars);

        // 从当前节点的下一节点继续执行
        String currentNodeId = instance.getCurrentNodeId();
        NodeModel nextNode = jsonParser.getNextNode(model, currentNodeId, allVars);

        if (nextNode == null) {
            // 流程完成
            completeProcess(instance);
            return;
        }

        // 继续执行
        executeFromNode(context, model, nextNode);
    }

    /**
     * 回退到指定节点（驳回场景）
     * 回退后从目标节点重新执行，触发 userTask 等等待节点的任务创建。
     */
    @Transactional
    public void rollback(Long instanceId, String targetNodeId) {
        ProcessInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND);
        }
        if (instance.getStatus() != ProcessStatus.RUNNING.getValue()) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID, "只有运行中的实例可以回退");
        }

        // 如果未指定目标节点，尝试从流程定义中查找"提交申请"节点（第一个 userTask）
        if (targetNodeId == null || targetNodeId.isBlank()) {
            targetNodeId = findSubmitNodeId(instance.getProcessKey());
        }
        if (targetNodeId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "无法确定回退目标节点");
        }

        instance.setCurrentNodeId(targetNodeId);
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        log.info("[FlowEngine] 流程实例 {} 回退到节点 {}", instanceId, targetNodeId);

        // 从目标节点重新执行（触发 userTask 任务创建等）
        ProcessDefinitionResponse defResp = definitionService.getByKey(instance.getProcessKey());
        ProcessModel model = jsonParser.parse(defResp.getProcessJson());
        NodeModel targetNode = findNode(model, targetNodeId);
        if (targetNode != null) {
            Map<String, Object> vars = variableService.getVariables(instanceId);
            ExecutionContext context = buildContext(instance, vars);
            executeFromNode(context, model, targetNode);
        }
    }

    /**
     * 查找流程定义中第一个 userTask 节点（作为"提交"节点的默认回退目标）
     */
    private String findSubmitNodeId(String processKey) {
        try {
            ProcessDefinitionResponse defResp = definitionService.getByKey(processKey);
            ProcessModel model = jsonParser.parse(defResp.getProcessJson());
            if (model.getNodes() != null) {
                return model.getNodes().stream()
                        .filter(n -> "userTask".equals(n.getType()))
                        .map(NodeModel::getId)
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("[FlowEngine] 查找提交节点失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 start 节点开始执行
     */
    private void executeFromStart(ExecutionContext context, ProcessModel model) {
        NodeModel startNode = model.getNodes().stream()
                .filter(n -> "start".equals(n.getType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_MISSING_START_NODE));

        executeFromNode(context, model, startNode);
    }

    /**
     * 从指定节点开始执行，自动推进直到遇到等待节点或完成
     */
    private void executeFromNode(ExecutionContext context, ProcessModel model, NodeModel currentNode) {
        int steps = 0;
        NodeModel node = currentNode;

        while (node != null && steps < MAX_STEPS) {
            steps++;
            context.setCurrentNodeId(node.getId());
            context.resetLocalScope();

            // 更新实例当前节点
            updateCurrentNode(context.getProcessInstanceId(), node.getId());

            // 执行节点
            NodeExecutor.ExecutionResult result = nodeExecutor.execute(context, node, model);

            if (result.isCompleted()) {
                // 流程完成
                ProcessInstance instance = instanceMapper.selectById(Long.parseLong(context.getProcessInstanceId()));
                if (instance != null) {
                    completeProcess(instance);
                }
                return;
            }

            if (result.isWaiting()) {
                // 等待外部触发（如 userTask）
                updateCurrentNode(context.getProcessInstanceId(), result.getTargetNodeId());
                log.info("[FlowEngine] 流程实例 {} 等待在节点 {}", context.getProcessInstanceId(), result.getTargetNodeId());
                return;
            }

            if (result.isMoveTo()) {
                // 移动到下一节点
                node = findNode(model, result.getTargetNodeId());
                if (node == null) {
                    log.warn("[FlowEngine] 目标节点 {} 不存在，流程结束", result.getTargetNodeId());
                    ProcessInstance instance = instanceMapper.selectById(Long.parseLong(context.getProcessInstanceId()));
                    if (instance != null) {
                        completeProcess(instance);
                    }
                    return;
                }
            }
        }

        if (steps >= MAX_STEPS) {
            log.warn("[FlowEngine] 流程实例 {} 执行步数超过上限 {}", context.getProcessInstanceId(), MAX_STEPS);
        }
    }

    private void completeProcess(ProcessInstance instance) {
        instance.setStatus(ProcessStatus.COMPLETED.getValue());
        instance.setEndTime(LocalDateTime.now());
        if (instance.getStartTime() != null) {
            instance.setDuration(Duration.between(instance.getStartTime(), instance.getEndTime()).toMillis());
        }
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        eventPublisher.publishEvent(new ProcessCompletedEvent(this, instance.getId(), instance.getProcessKey()));
        log.info("[FlowEngine] 流程实例 {} 已完成", instance.getId());
    }

    private void updateCurrentNode(String instanceId, String nodeId) {
        try {
            ProcessInstance instance = instanceMapper.selectById(Long.parseLong(instanceId));
            if (instance != null) {
                instance.setCurrentNodeId(nodeId);
                instance.setUpdateTime(LocalDateTime.now());
                instanceMapper.updateById(instance);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析实例ID: {}", instanceId);
        }
    }

    private ExecutionContext buildContext(ProcessInstance instance, Map<String, Object> variables) {
        ExecutionContext context = new ExecutionContext();
        context.setProcessInstanceId(String.valueOf(instance.getId()));
        context.setOperator(instance.getStartUser());
        if (variables != null) {
            variables.forEach(context::setVariable);
        }
        return context;
    }

    private NodeModel findNode(ProcessModel model, String nodeId) {
        if (model.getNodes() == null) return null;
        return model.getNodes().stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }
}
