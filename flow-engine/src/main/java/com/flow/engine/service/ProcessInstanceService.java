package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.ProcessStatus;
import com.flow.engine.dto.ProcessInstanceResponse;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.engine.FlowEngine;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.mapper.ProcessInstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
        return response;
    }
}
