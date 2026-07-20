package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.OperationLog;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.entity.Task;
import com.flow.engine.entity.Variable;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.mapper.TaskMapper;
import com.flow.engine.mapper.VariableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程监控服务（ISSUE-017）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMonitorService {

    private final ProcessInstanceMapper processInstanceMapper;
    private final TaskMapper taskMapper;
    private final VariableMapper variableMapper;
    private final LogService logService;

    /**
     * 获取执行轨迹/历史
     */
    public List<Map<String, Object>> getExecutionHistory(Long processInstanceId) {
        // 查询流程实例
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        }
        
        // 查询该实例的所有任务
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, processInstanceId)
                        .orderByAsc(Task::getCreateTime)
        );
        
        // 构建执行轨迹
        List<Map<String, Object>> history = new ArrayList<>();
        
        // 添加开始节点
        Map<String, Object> startNode = new HashMap<>();
        startNode.put("nodeId", "start");
        startNode.put("nodeName", "开始");
        startNode.put("nodeType", "start");
        startNode.put("enterTime", instance.getStartTime());
        startNode.put("duration", 0);
        startNode.put("status", "completed");
        history.add(startNode);
        
        // 添加任务节点
        for (Task task : tasks) {
            Map<String, Object> node = new HashMap<>();
            node.put("nodeId", task.getNodeId());
            node.put("nodeName", task.getNodeName());
            node.put("nodeType", "userTask");
            node.put("taskId", task.getId());
            node.put("assignee", task.getAssignee());
            node.put("enterTime", task.getCreateTime());
            node.put("completeTime", task.getCompleteTime());
            
            // 计算节点耗时
            if (task.getCompleteTime() != null && task.getCreateTime() != null) {
                long duration = Duration.between(task.getCreateTime(), task.getCompleteTime()).toMillis();
                node.put("duration", duration);
            } else {
                node.put("duration", null);
            }
            
            // 任务状态
            String status = switch (task.getStatus()) {
                case 0 -> "pending";
                case 1 -> "processing";
                case 2 -> "completed";
                default -> "unknown";
            };
            node.put("status", status);
            
            history.add(node);
        }
        
        // 添加结束节点（如果流程已完成）
        if (instance.getStatus() != null && instance.getStatus() == 1) {
            Map<String, Object> endNode = new HashMap<>();
            endNode.put("nodeId", "end");
            endNode.put("nodeName", "结束");
            endNode.put("nodeType", "end");
            endNode.put("enterTime", instance.getEndTime());
            endNode.put("duration", 0);
            endNode.put("status", "completed");
            history.add(endNode);
        }
        
        return history;
    }

    /**
     * 获取变量历史
     */
    public List<Map<String, Object>> getVariableHistory(Long processInstanceId) {
        // 查询流程实例
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        }
        
        // 查询该实例的所有变量
        List<Variable> variables = variableMapper.selectList(
                new LambdaQueryWrapper<Variable>()
                        .eq(Variable::getProcessInstanceId, processInstanceId)
                        .orderByAsc(Variable::getCreateTime)
        );
        
        return variables.stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", v.getId());
            map.put("variableKey", v.getVariableKey());
            map.put("variableValue", v.getVariableValue());
            map.put("variableType", v.getVariableType());
            map.put("taskId", v.getTaskId());
            map.put("createTime", v.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取耗时统计
     */
    public Map<String, Object> getStatistics(Long processInstanceId) {
        // 查询流程实例
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        // 实例总耗时
        if (instance.getDuration() != null) {
            stats.put("totalDuration", instance.getDuration());
        } else if (instance.getStartTime() != null) {
            LocalDateTime endTime = instance.getEndTime() != null ? instance.getEndTime() : LocalDateTime.now();
            long duration = Duration.between(instance.getStartTime(), endTime).toMillis();
            stats.put("totalDuration", duration);
        }
        
        // 各节点耗时
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, processInstanceId)
                        .orderByAsc(Task::getCreateTime)
        );
        
        List<Map<String, Object>> nodeStats = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> nodeStat = new HashMap<>();
            nodeStat.put("nodeId", task.getNodeId());
            nodeStat.put("nodeName", task.getNodeName());
            nodeStat.put("taskId", task.getId());
            
            if (task.getCompleteTime() != null && task.getCreateTime() != null) {
                long duration = Duration.between(task.getCreateTime(), task.getCompleteTime()).toMillis();
                nodeStat.put("duration", duration);
            } else if (task.getCreateTime() != null) {
                long duration = Duration.between(task.getCreateTime(), LocalDateTime.now()).toMillis();
                nodeStat.put("duration", duration);
                nodeStat.put("ongoing", true);
            }
            
            nodeStats.add(nodeStat);
        }
        stats.put("nodeStatistics", nodeStats);
        
        // 任务统计
        long completedTasks = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 2).count();
        long pendingTasks = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 0).count();
        long processingTasks = tasks.stream().filter(t -> t.getStatus() != null && t.getStatus() == 1).count();
        
        stats.put("totalTasks", tasks.size());
        stats.put("completedTasks", completedTasks);
        stats.put("pendingTasks", pendingTasks);
        stats.put("processingTasks", processingTasks);
        
        return stats;
    }

    /**
     * 获取运行中的流程列表
     */
    public List<Map<String, Object>> getRunningProcesses(String processKey, String startUser, 
                                                         LocalDateTime startTime, LocalDateTime endTime,
                                                         int page, int size) {
        LambdaQueryWrapper<ProcessInstance> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询运行中的实例（status=0）
        wrapper.eq(ProcessInstance::getStatus, 0);
        
        if (StringUtils.hasText(processKey)) {
            wrapper.eq(ProcessInstance::getProcessKey, processKey);
        }
        if (StringUtils.hasText(startUser)) {
            wrapper.like(ProcessInstance::getStartUser, startUser);
        }
        if (startTime != null) {
            wrapper.ge(ProcessInstance::getStartTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(ProcessInstance::getStartTime, endTime);
        }
        
        wrapper.orderByDesc(ProcessInstance::getStartTime);
        
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + size + " OFFSET " + offset);
        
        List<ProcessInstance> instances = processInstanceMapper.selectList(wrapper);
        
        return instances.stream().map(instance -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", instance.getId());
            map.put("processKey", instance.getProcessKey());
            map.put("processName", instance.getProcessName());
            map.put("processVersion", instance.getProcessVersion());
            map.put("businessKey", instance.getBusinessKey());
            map.put("currentNodeId", instance.getCurrentNodeId());
            map.put("startUser", instance.getStartUser());
            map.put("startTime", instance.getStartTime());
            
            // 计算运行时长
            if (instance.getStartTime() != null) {
                long duration = Duration.between(instance.getStartTime(), LocalDateTime.now()).toMillis();
                map.put("runningDuration", duration);
            }
            
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 导出实例数据
     */
    public Map<String, Object> exportInstanceData(Long processInstanceId) {
        // 查询流程实例
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        }
        
        Map<String, Object> exportData = new HashMap<>();
        
        // 基本信息
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("id", instance.getId());
        basicInfo.put("processKey", instance.getProcessKey());
        basicInfo.put("processName", instance.getProcessName());
        basicInfo.put("processVersion", instance.getProcessVersion());
        basicInfo.put("businessKey", instance.getBusinessKey());
        basicInfo.put("status", instance.getStatus());
        basicInfo.put("startUser", instance.getStartUser());
        basicInfo.put("startTime", instance.getStartTime());
        basicInfo.put("endTime", instance.getEndTime());
        basicInfo.put("duration", instance.getDuration());
        exportData.put("basicInfo", basicInfo);
        
        // 执行轨迹
        exportData.put("executionHistory", getExecutionHistory(processInstanceId));
        
        // 变量历史
        exportData.put("variableHistory", getVariableHistory(processInstanceId));
        
        // 耗时统计
        exportData.put("statistics", getStatistics(processInstanceId));
        
        return exportData;
    }

    /**
     * 管理员干预：强制推进到指定节点
     */
    public void intervene(Long processInstanceId, String targetNodeId, Long operatorId, String reason) {
        log.info("[ProcessMonitor] 管理员干预: instanceId={}, targetNode={}, operator={}, reason={}", 
                processInstanceId, targetNodeId, operatorId, reason);
        
        // 查询流程实例
        ProcessInstance instance = processInstanceMapper.selectById(processInstanceId);
        if (instance == null) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_FOUND);
        }
        
        // 检查流程是否在运行
        if (instance.getStatus() == null || instance.getStatus() != 0) {
            throw new BusinessException(ErrorCode.INSTANCE_NOT_RUNNING);
        }
        
        // 更新当前节点
        instance.setCurrentNodeId(targetNodeId);
        instance.setUpdateTime(LocalDateTime.now());
        processInstanceMapper.updateById(instance);
        
        // 记录操作日志
        OperationLog operationLog = new OperationLog();
        operationLog.setModule("流程监控");
        operationLog.setOperation("管理员干预");
        operationLog.setUserId(operatorId);
        operationLog.setMethod("intervene");
        operationLog.setParams("instanceId=" + processInstanceId + ", targetNode=" + targetNodeId);
        operationLog.setResult("成功");
        operationLog.setBeforeData("当前节点: " + instance.getCurrentNodeId());
        operationLog.setAfterData("目标节点: " + targetNodeId);
        operationLog.setOperationTime(LocalDateTime.now());
        logService.recordOperationLog(operationLog);
        
        log.info("[ProcessMonitor] 管理员干预成功: instanceId={}, targetNode={}", processInstanceId, targetNodeId);
    }
}
