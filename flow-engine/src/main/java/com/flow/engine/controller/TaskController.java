package com.flow.engine.controller;

import com.flow.engine.common.Result;
import com.flow.engine.dto.*;
import com.flow.engine.service.FormPermissionService;
import com.flow.engine.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 任务控制器（ISSUE-005，TRD §3.3）
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@CrossOrigin
public class TaskController {

    private final TaskService taskService;
    private final FormPermissionService formPermissionService;

    /**
     * 获取待办任务列表
     * GET /api/v1/tasks/todo?userId=xxx
     */
    @GetMapping("/todo")
    public Result<List<TaskResponse>> todo(@RequestParam String userId) {
        return Result.ok(taskService.getTodoList(userId));
    }

    /**
     * 获取已办任务列表
     * GET /api/v1/tasks/done?userId=xxx
     */
    @GetMapping("/done")
    public Result<List<TaskResponse>> done(@RequestParam String userId) {
        return Result.ok(taskService.getDoneList(userId));
    }

    /**
     * 获取任务详情
     * GET /api/v1/tasks/{id}
     */
    @GetMapping("/{id}")
    public Result<TaskResponse> getById(@PathVariable Long id) {
        return Result.ok(taskService.getById(id));
    }

    /**
     * 获取指定流程实例的所有任务（用于流程图节点状态）
     * GET /api/v1/tasks/instance/{instanceId}
     */
    @GetMapping("/instance/{instanceId}")
    public Result<List<TaskResponse>> getByInstance(@PathVariable Long instanceId) {
        return Result.ok(taskService.getTasksByInstance(instanceId));
    }

    /**
     * 签收任务
     * POST /api/v1/tasks/{id}/claim
     */
    @PostMapping("/{id}/claim")
    public Result<TaskResponse> claim(@PathVariable Long id, @RequestBody ClaimTaskRequest request) {
        return Result.ok(taskService.claim(id, request.getUserId()));
    }

    /**
     * 取消签收
     * POST /api/v1/tasks/{id}/unclaim
     */
    @PostMapping("/{id}/unclaim")
    public Result<TaskResponse> unclaim(@PathVariable Long id) {
        return Result.ok(taskService.unclaim(id));
    }

    /**
     * 完成任务（通过）
     * POST /api/v1/tasks/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public Result<TaskResponse> complete(@PathVariable Long id,
                                         @RequestBody(required = false) Map<String, Object> body) {
        String userId = body != null ? (String) body.get("userId") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = body != null ? (Map<String, Object>) body.get("variables") : null;
        return Result.ok(taskService.complete(id, userId, variables));
    }

    /**
     * 驳回任务
     * POST /api/v1/tasks/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public Result<TaskResponse> reject(@PathVariable Long id, @RequestBody RejectTaskRequest request) {
        return Result.ok(taskService.reject(id, request.getUserId(), request.getTargetNodeId(), request.getVariables()));
    }

    /**
     * 转办任务
     * POST /api/v1/tasks/{id}/transfer
     */
    @PostMapping("/{id}/transfer")
    public Result<TaskResponse> transfer(@PathVariable Long id, @RequestBody TransferTaskRequest request) {
        return Result.ok(taskService.transfer(id, request.getOperatorId(), request.getTargetUserId()));
    }

    /**
     * 委派任务
     * POST /api/v1/tasks/{id}/delegate
     */
    @PostMapping("/{id}/delegate")
    public Result<TaskResponse> delegate(@PathVariable Long id, @RequestBody DelegateTaskRequest request) {
        return Result.ok(taskService.delegate(id, request.getOperatorId(), request.getDelegateUserId()));
    }

    /**
     * 获取任务表单权限（ISSUE-009，TRD §3.4）
     * GET /api/v1/tasks/{taskId}/form-permissions
     */
    @GetMapping("/{taskId}/form-permissions")
    public Result<FormPermissionResponse> getFormPermissions(@PathVariable Long taskId) {
        return Result.ok(formPermissionService.getFormPermissions(taskId));
    }
}
