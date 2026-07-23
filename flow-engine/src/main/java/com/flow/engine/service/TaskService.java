package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.common.enums.TaskAction;
import com.flow.engine.common.enums.TaskStatus;
import com.flow.engine.dto.TaskResponse;
import com.flow.engine.engine.FlowEngine;
import com.flow.engine.entity.Task;
import com.flow.engine.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务服务（ISSUE-005，TRD §4.2）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final FlowEngine flowEngine;
    private final CacheManager cacheManager;

    private static final String TODO_CACHE_PREFIX = "task:todo:";

    /**
     * 创建任务
     */
    @Transactional
    public Task createTask(Long processInstanceId, String processKey,
                           String nodeId, String nodeName,
                           String assignee, String candidateUsers) {
        Task task = new Task();
        task.setProcessInstanceId(processInstanceId);
        task.setProcessKey(processKey);
        task.setNodeId(nodeId);
        task.setNodeName(nodeName);
        task.setTaskType(1); // 普通任务
        task.setAssignee(assignee);
        task.setCandidateUsers(candidateUsers);
        task.setTaskAction(TaskAction.NORMAL.getValue());
        task.setStatus(TaskStatus.PENDING.getValue());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);

        // 清除相关缓存
        evictTodoCache(assignee);
        evictCandidateCache(candidateUsers);

        log.info("[TaskService] 创建任务: id={}, node={}, assignee={}", task.getId(), nodeId, assignee);
        return task;
    }

    /**
     * 获取任务详情
     */
    public TaskResponse getById(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return toResponse(task);
    }

    /**
     * 获取指定流程实例的所有任务（用于流程图节点状态展示）
     */
    public List<TaskResponse> getTasksByInstance(Long processInstanceId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProcessInstanceId, processInstanceId)
                .orderByAsc(Task::getCreateTime);
        return taskMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取待办任务列表
     */
    public List<TaskResponse> getTodoList(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "userId不能为空");
        }

        // 尝试从缓存获取
        Cache cache = cacheManager.getCache(TODO_CACHE_PREFIX + userId);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get("list");
            if (cached != null) {
                @SuppressWarnings("unchecked")
                List<TaskResponse> result = (List<TaskResponse>) cached.get();
                return result;
            }
        }

        // 查 DB：(assignee=userId OR candidateUsers包含userId) 且 status!=已完成
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(Task::getAssignee, userId)
                .or()
                .apply("candidate_users LIKE {0}", "%" + userId + "%"))
                .ne(Task::getStatus, TaskStatus.COMPLETED.getValue())
                .orderByDesc(Task::getCreateTime);

        List<TaskResponse> result = taskMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        // 回填缓存
        if (cache != null) {
            cache.put("list", result);
        }
        return result;
    }

    /**
     * 获取已办任务列表
     */
    public List<TaskResponse> getDoneList(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "userId不能为空");
        }

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(Task::getAssignee, userId)
                .or()
                .apply("candidate_users LIKE {0}", "%" + userId + "%"))
                .eq(Task::getStatus, TaskStatus.COMPLETED.getValue())
                .orderByDesc(Task::getCompleteTime);

        return taskMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 签收任务
     */
    @Transactional
    public TaskResponse claim(Long taskId, String userId) {
        Task task = getTaskOrThrow(taskId);

        if (task.getStatus() != TaskStatus.PENDING.getValue()) {
            throw new BusinessException(ErrorCode.TASK_NOT_PENDING, "只有待处理任务才能签收");
        }
        if (StringUtils.hasText(task.getAssignee()) && !task.getAssignee().equals(userId)) {
            // 已有签收人
            throw new BusinessException(ErrorCode.TASK_ALREADY_CLAIMED);
        }

        // 校验用户是否在候选人列表中
        if (StringUtils.hasText(task.getCandidateUsers())) {
            List<String> candidates = Arrays.asList(task.getCandidateUsers().split(","));
            if (!candidates.contains(userId)) {
                throw new BusinessException(ErrorCode.TASK_NOT_ASSIGNEE, "用户不在候选人列表中");
            }
        }

        task.setAssignee(userId);
        task.setStatus(TaskStatus.IN_PROGRESS.getValue());
        task.setClaimTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTodoCache(userId);

        log.info("[TaskService] 任务 {} 被 {} 签收", taskId, userId);
        return toResponse(task);
    }

    /**
     * 取消签收
     */
    @Transactional
    public TaskResponse unclaim(Long taskId) {
        Task task = getTaskOrThrow(taskId);

        if (task.getStatus() != TaskStatus.IN_PROGRESS.getValue()) {
            throw new BusinessException(ErrorCode.TASK_NOT_ASSIGNEE, "只有处理中的任务才能取消签收");
        }

        String oldAssignee = task.getAssignee();
        task.setAssignee(null);
        task.setStatus(TaskStatus.PENDING.getValue());
        task.setClaimTime(null);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTodoCache(oldAssignee);

        log.info("[TaskService] 任务 {} 取消签收", taskId);
        return toResponse(task);
    }

    /**
     * 完成任务（通过）
     */
    @Transactional
    public TaskResponse complete(Long taskId, String userId, Map<String, Object> variables) {
        Task task = getTaskOrThrow(taskId);
        validateOperator(task, userId);

        task.setStatus(TaskStatus.COMPLETED.getValue());
        task.setTaskAction(TaskAction.APPROVED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTodoCache(userId);

        // 推进流程
        flowEngine.completeTask(task.getProcessInstanceId(), variables);

        log.info("[TaskService] 任务 {} 被 {} 通过", taskId, userId);
        return toResponse(task);
    }

    /**
     * 驳回任务
     */
    @Transactional
    public TaskResponse reject(Long taskId, String userId, String targetNodeId, Map<String, Object> variables) {
        Task task = getTaskOrThrow(taskId);
        validateOperator(task, userId);

        task.setStatus(TaskStatus.COMPLETED.getValue());
        task.setTaskAction(TaskAction.REJECTED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTodoCache(userId);

        // 回退流程到目标节点（默认回退到申请人提交节点）
        flowEngine.rollback(task.getProcessInstanceId(), targetNodeId);

        log.info("[TaskService] 任务 {} 被 {} 驳回，回退到节点 {}", taskId, userId, targetNodeId);
        return toResponse(task);
    }

    /**
     * 转办任务
     */
    @Transactional
    public TaskResponse transfer(Long taskId, String operatorId, String targetUserId) {
        Task task = getTaskOrThrow(taskId);
        validateOperator(task, operatorId);

        // 原任务标记为转办完成
        task.setStatus(TaskStatus.COMPLETED.getValue());
        task.setTaskAction(TaskAction.TRANSFERRED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 创建新任务给目标用户
        Task newTask = createTask(
                task.getProcessInstanceId(),
                task.getProcessKey(),
                task.getNodeId(),
                task.getNodeName(),
                targetUserId,
                null
        );
        newTask.setStatus(TaskStatus.PENDING.getValue());
        newTask.setTaskAction(TaskAction.NORMAL.getValue());
        taskMapper.updateById(newTask);

        evictTodoCache(operatorId);
        evictTodoCache(targetUserId);

        log.info("[TaskService] 任务 {} 由 {} 转办给 {}", taskId, operatorId, targetUserId);
        return toResponse(newTask);
    }

    /**
     * 委派任务
     */
    @Transactional
    public TaskResponse delegate(Long taskId, String operatorId, String delegateUserId) {
        Task task = getTaskOrThrow(taskId);
        validateOperator(task, operatorId);

        // 原任务标记为委派完成
        task.setStatus(TaskStatus.COMPLETED.getValue());
        task.setTaskAction(TaskAction.DELEGATED.getValue());
        task.setCompleteTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        // 创建新任务给受托人
        Task newTask = createTask(
                task.getProcessInstanceId(),
                task.getProcessKey(),
                task.getNodeId(),
                task.getNodeName(),
                delegateUserId,
                null
        );
        newTask.setStatus(TaskStatus.PENDING.getValue());
        newTask.setTaskAction(TaskAction.NORMAL.getValue());
        taskMapper.updateById(newTask);

        evictTodoCache(operatorId);
        evictTodoCache(delegateUserId);

        log.info("[TaskService] 任务 {} 由 {} 委派给 {}", taskId, operatorId, delegateUserId);
        return toResponse(newTask);
    }

    /**
     * 根据流程实例ID和节点ID获取任务
     */
    public Task getByNode(Long processInstanceId, String nodeId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProcessInstanceId, processInstanceId)
                .eq(Task::getNodeId, nodeId)
                .ne(Task::getStatus, TaskStatus.COMPLETED.getValue())
                .orderByDesc(Task::getCreateTime)
                .last("LIMIT 1");
        return taskMapper.selectOne(wrapper);
    }

    /**
     * 取消流程实例的所有待办任务（流程终止时调用）
     */
    @Transactional
    public void cancelPendingTasks(Long processInstanceId) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getProcessInstanceId, processInstanceId)
                .ne(Task::getStatus, TaskStatus.COMPLETED.getValue());
        List<Task> pendingTasks = taskMapper.selectList(wrapper);
        for (Task task : pendingTasks) {
            task.setStatus(TaskStatus.COMPLETED.getValue());
            task.setTaskAction(TaskAction.CANCELLED.getValue());
            task.setCompleteTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            taskMapper.updateById(task);
            evictTodoCache(task.getAssignee());
            log.info("[TaskService] 取消任务: id={}, processInstanceId={}", task.getId(), processInstanceId);
        }
    }

    // ==================== 私有方法 ====================

    private Task getTaskOrThrow(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    /**
     * 校验操作人权限：签收后仅签收人可操作；未签收时候选人可签收
     */
    private void validateOperator(Task task, String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "操作人不能为空");
        }
        if (task.getStatus() == TaskStatus.COMPLETED.getValue()) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "任务已完成");
        }
        // 如果任务已签收，只有签收人可操作
        if (StringUtils.hasText(task.getAssignee()) && !task.getAssignee().equals(userId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_ASSIGNEE);
        }
        // 如果未签收，检查候选人
        if (!StringUtils.hasText(task.getAssignee()) && StringUtils.hasText(task.getCandidateUsers())) {
            List<String> candidates = Arrays.asList(task.getCandidateUsers().split(","));
            if (!candidates.contains(userId)) {
                throw new BusinessException(ErrorCode.TASK_NOT_ASSIGNEE, "用户不在候选人列表中");
            }
        }
    }

    private void evictTodoCache(String userId) {
        if (StringUtils.hasText(userId)) {
            Cache cache = cacheManager.getCache(TODO_CACHE_PREFIX + userId);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void evictCandidateCache(String candidateUsers) {
        if (StringUtils.hasText(candidateUsers)) {
            for (String user : candidateUsers.split(",")) {
                evictTodoCache(user.trim());
            }
        }
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse resp = new TaskResponse();
        resp.setId(task.getId());
        resp.setProcessInstanceId(task.getProcessInstanceId());
        resp.setProcessKey(task.getProcessKey());
        resp.setNodeId(task.getNodeId());
        resp.setNodeName(task.getNodeName());
        resp.setTaskType(task.getTaskType());
        resp.setAssignee(task.getAssignee());
        resp.setCandidateUsers(task.getCandidateUsers());
        resp.setClaimTime(task.getClaimTime());
        resp.setCompleteTime(task.getCompleteTime());
        resp.setTaskAction(task.getTaskAction());
        TaskAction action = TaskAction.fromValue(task.getTaskAction() != null ? task.getTaskAction() : 0);
        resp.setTaskActionDesc(action.getDesc());
        resp.setStatus(task.getStatus());
        TaskStatus status = TaskStatus.fromValue(task.getStatus());
        resp.setStatusDesc(status != null ? status.getDesc() : "未知");
        resp.setCreateTime(task.getCreateTime());
        resp.setUpdateTime(task.getUpdateTime());
        return resp;
    }
}
