package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.ProcessStatus;
import com.flow.engine.dto.ProcessDefinitionResponse;
import com.flow.engine.dto.ProcessInstanceResponse;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.engine.FlowEngine;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.model.NodeModel;
import com.flow.engine.model.ProcessModel;
import com.flow.engine.parser.ProcessJsonParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例服务（ISSUE-004）
 */
@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private final ProcessInstanceMapper instanceMapper;
    private final FlowEngine flowEngine;
    private final VariableService variableService;
    private final TaskService taskService;
    private final ProcessDefinitionService definitionService;
    private final ProcessJsonParser jsonParser;

    /**
     * 发起流程实例
     */
    @Transactional
    public ProcessInstanceResponse start(StartProcessRequest request) {
        if (!StringUtils.hasText(request.getProcessKey())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "processKey不能为空");
        }

        ProcessInstance instance = flowEngine.startProcess(
                request.getProcessKey(),
                request.getBusinessKey(),
                request.getStartUser(),
                request.getVariables()
        );
        return toResponse(instance);
    }

    /**
     * 获取实例详情
     */
    public ProcessInstanceResponse getById(Long id) {
        ProcessInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND);
        }
        return toResponse(instance);
    }

    /**
     * 获取实例列表
     */
    public List<ProcessInstanceResponse> list(String processKey, Integer status) {
        LambdaQueryWrapper<ProcessInstance> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(processKey)) {
            wrapper.eq(ProcessInstance::getProcessKey, processKey);
        }
        if (status != null) {
            wrapper.eq(ProcessInstance::getStatus, status);
        }
        wrapper.orderByDesc(ProcessInstance::getCreateTime);

        return instanceMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取某用户发起的流程实例
     */
    public List<ProcessInstanceResponse> listByStartUser(String startUser) {
        if (!StringUtils.hasText(startUser)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "startUser不能为空");
        }
        LambdaQueryWrapper<ProcessInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessInstance::getStartUser, startUser)
               .orderByDesc(ProcessInstance::getCreateTime);
        List<ProcessInstance> instances = instanceMapper.selectList(wrapper);

        // 批量解析节点名称：按 processKey 分组，避免重复解析
        Map<String, Map<String, String>> nodeNamesCache = new HashMap<>();
        for (ProcessInstance inst : instances) {
            if (inst.getProcessKey() != null && !nodeNamesCache.containsKey(inst.getProcessKey())) {
                nodeNamesCache.put(inst.getProcessKey(), resolveNodeNames(inst.getProcessKey()));
            }
        }

        return instances.stream()
                .map(inst -> {
                    ProcessInstanceResponse resp = toResponse(inst);
                    Map<String, String> nameMap = nodeNamesCache.getOrDefault(inst.getProcessKey(), Map.of());
                    if (inst.getCurrentNodeId() != null) {
                        resp.setCurrentNodeName(nameMap.getOrDefault(inst.getCurrentNodeId(), inst.getCurrentNodeId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据流程定义 processKey 解析所有节点的 id→name 映射
     */
    private Map<String, String> resolveNodeNames(String processKey) {
        try {
            ProcessDefinitionResponse def = definitionService.getByKey(processKey);
            if (def != null && def.getProcessJson() != null) {
                ProcessModel model = jsonParser.parse(def.getProcessJson());
                if (model.getNodes() != null) {
                    Map<String, String> map = new HashMap<>();
                    for (NodeModel n : model.getNodes()) {
                        map.put(n.getId(), n.getName() != null ? n.getName() : n.getId());
                    }
                    return map;
                }
            }
        } catch (Exception e) {
            // 解析失败时返回空映射，前端回退显示 nodeId
        }
        return Map.of();
    }

    /**
     * 暂停流程实例
     */
    @Transactional
    public ProcessInstanceResponse suspend(Long id) {
        ProcessInstance instance = getInstanceOrThrow(id);
        validateTransition(instance, ProcessStatus.SUSPENDED);

        instance.setStatus(ProcessStatus.SUSPENDED.getValue());
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        return toResponse(instance);
    }

    /**
     * 恢复流程实例
     */
    @Transactional
    public ProcessInstanceResponse resume(Long id) {
        ProcessInstance instance = getInstanceOrThrow(id);
        validateTransition(instance, ProcessStatus.RUNNING);

        instance.setStatus(ProcessStatus.RUNNING.getValue());
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);
        return toResponse(instance);
    }

    /**
     * 终止流程实例（不可逆）
     */
    @Transactional
    public ProcessInstanceResponse terminate(Long id) {
        ProcessInstance instance = getInstanceOrThrow(id);
        validateTransition(instance, ProcessStatus.TERMINATED);

        instance.setStatus(ProcessStatus.TERMINATED.getValue());
        instance.setEndTime(LocalDateTime.now());
        if (instance.getStartTime() != null) {
            instance.setDuration(java.time.Duration.between(instance.getStartTime(), instance.getEndTime()).toMillis());
        }
        instance.setUpdateTime(LocalDateTime.now());
        instanceMapper.updateById(instance);

        // 取消所有待办任务
        taskService.cancelPendingTasks(id);

        return toResponse(instance);
    }

    /**
     * 获取流程变量
     */
    public Map<String, Object> getVariables(Long instanceId) {
        getInstanceOrThrow(instanceId);
        return variableService.getVariables(instanceId);
    }

    /**
     * 更新流程变量
     */
    @Transactional
    public void updateVariables(Long instanceId, Map<String, Object> variables) {
        getInstanceOrThrow(instanceId);
        variableService.updateVariables(instanceId, variables);
    }

    /**
     * 完成任务并推进
     */
    @Transactional
    public void completeTask(Long instanceId, Map<String, Object> variables) {
        flowEngine.completeTask(instanceId, variables);
    }

    private ProcessInstance getInstanceOrThrow(Long id) {
        ProcessInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw new BusinessException(ErrorCode.PROCESS_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    private void validateTransition(ProcessInstance instance, ProcessStatus target) {
        ProcessStatus current = ProcessStatus.fromValue(instance.getStatus());
        if (current == null || !current.canTransitionTo(target)) {
            throw new BusinessException(ErrorCode.STATUS_TRANSITION_INVALID,
                    "当前状态 [" + (current != null ? current.getDesc() : "未知") + "] 不允许转换到 [" + target.getDesc() + "]");
        }
    }

    private ProcessInstanceResponse toResponse(ProcessInstance instance) {
        ProcessInstanceResponse response = new ProcessInstanceResponse();
        response.setId(instance.getId());
        response.setInstanceNo(instance.getInstanceNo());
        response.setProcessKey(instance.getProcessKey());
        response.setProcessName(instance.getProcessName());
        response.setProcessVersion(instance.getProcessVersion());
        response.setBusinessKey(instance.getBusinessKey());
        response.setStatus(instance.getStatus());
        ProcessStatus status = ProcessStatus.fromValue(instance.getStatus());
        response.setStatusDesc(status != null ? status.getDesc() : "未知");
        response.setCurrentNodeId(instance.getCurrentNodeId());
        response.setStartUser(instance.getStartUser());
        response.setStartTime(instance.getStartTime());
        response.setEndTime(instance.getEndTime());
        response.setDuration(instance.getDuration());
        response.setVersion(instance.getVersion());
        response.setCreateTime(instance.getCreateTime());
        response.setUpdateTime(instance.getUpdateTime());

        // 解析 processType
        try {
            ProcessDefinitionResponse def = definitionService.getByKey(instance.getProcessKey());
            if (def != null) {
                response.setProcessType(def.getProcessType());
            }
        } catch (Exception ignored) {}

        return response;
    }
}
