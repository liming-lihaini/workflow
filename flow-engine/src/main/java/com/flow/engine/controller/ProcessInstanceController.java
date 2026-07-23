package com.flow.engine.controller;

import com.flow.engine.common.Result;
import com.flow.engine.dto.CompleteTaskRequest;
import com.flow.engine.dto.ProcessInstanceResponse;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 流程实例控制器（ISSUE-004，TRD §3.2）
 */
@RestController
@RequestMapping("/api/v1/process/instances")
@RequiredArgsConstructor
@CrossOrigin
public class ProcessInstanceController {

    private final ProcessInstanceService instanceService;

    /**
     * 发起流程实例
     * POST /api/v1/process/instances
     */
    @PostMapping
    public Result<ProcessInstanceResponse> start(@RequestBody StartProcessRequest request) {
        ProcessInstanceResponse response = instanceService.start(request);
        return Result.ok(response);
    }

    /**
     * 获取流程实例列表
     * GET /api/v1/process/instances
     */
    @GetMapping
    public Result<List<ProcessInstanceResponse>> list(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) Integer status) {
        List<ProcessInstanceResponse> list = instanceService.list(processKey, status);
        return Result.ok(list);
    }

    /**
     * 获取我发起的流程实例
     * GET /api/v1/process/instances/my?startUser=xxx
     */
    @GetMapping("/my")
    public Result<List<ProcessInstanceResponse>> myList(@RequestParam String startUser) {
        List<ProcessInstanceResponse> list = instanceService.listByStartUser(startUser);
        return Result.ok(list);
    }

    /**
     * 获取流程实例详情
     * GET /api/v1/process/instances/{id}
     */
    @GetMapping("/{id}")
    public Result<ProcessInstanceResponse> getById(@PathVariable Long id) {
        ProcessInstanceResponse response = instanceService.getById(id);
        return Result.ok(response);
    }

    /**
     * 暂停流程实例
     * POST /api/v1/process/instances/{id}/suspend
     */
    @PostMapping("/{id}/suspend")
    public Result<ProcessInstanceResponse> suspend(@PathVariable Long id) {
        ProcessInstanceResponse response = instanceService.suspend(id);
        return Result.ok(response);
    }

    /**
     * 恢复流程实例
     * POST /api/v1/process/instances/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public Result<ProcessInstanceResponse> resume(@PathVariable Long id) {
        ProcessInstanceResponse response = instanceService.resume(id);
        return Result.ok(response);
    }

    /**
     * 终止流程实例
     * POST /api/v1/process/instances/{id}/terminate
     */
    @PostMapping("/{id}/terminate")
    public Result<ProcessInstanceResponse> terminate(@PathVariable Long id) {
        ProcessInstanceResponse response = instanceService.terminate(id);
        return Result.ok(response);
    }

    /**
     * 获取流程变量
     * GET /api/v1/process/instances/{id}/variables
     */
    @GetMapping("/{id}/variables")
    public Result<Map<String, Object>> getVariables(@PathVariable Long id) {
        Map<String, Object> variables = instanceService.getVariables(id);
        return Result.ok(variables);
    }

    /**
     * 更新流程变量
     * PUT /api/v1/process/instances/{id}/variables
     */
    @PutMapping("/{id}/variables")
    public Result<Void> updateVariables(@PathVariable Long id, @RequestBody Map<String, Object> variables) {
        instanceService.updateVariables(id, variables);
        return Result.ok();
    }

    /**
     * 完成任务并推进
     * POST /api/v1/process/instances/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeTask(@PathVariable Long id, @RequestBody(required = false) CompleteTaskRequest request) {
        Map<String, Object> variables = request != null ? request.getVariables() : null;
        instanceService.completeTask(id, variables);
        return Result.ok();
    }
}
