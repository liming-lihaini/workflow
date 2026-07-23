package com.flow.engine.controller;

import com.flow.engine.common.Result;
import com.flow.engine.dto.*;
import com.flow.engine.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程定义控制器（ISSUE-003，TRD §3.1）
 */
@RestController
@RequestMapping("/api/v1/process/definitions")
@RequiredArgsConstructor
@CrossOrigin
public class ProcessController {

    private final ProcessDefinitionService definitionService;

    /**
     * 创建流程定义
     * POST /api/v1/process/definitions
     */
    @PostMapping
    public Result<ProcessDefinitionResponse> create(@RequestBody ProcessDefinitionCreateRequest request) {
        ProcessDefinitionResponse response = definitionService.create(request);
        return Result.ok(response);
    }

    /**
     * 获取流程定义列表
     * GET /api/v1/process/definitions
     */
    @GetMapping
    public Result<List<ProcessDefinitionResponse>> list(
            @RequestParam(required = false) String processKey,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        List<ProcessDefinitionResponse> list = definitionService.list(processKey, category, status);
        return Result.ok(list);
    }

    /**
     * 获取流程定义详情
     * GET /api/v1/process/definitions/{id}
     */
    @GetMapping("/{id}")
    public Result<ProcessDefinitionResponse> getById(@PathVariable Long id) {
        ProcessDefinitionResponse response = definitionService.getById(id);
        return Result.ok(response);
    }

    /**
     * 根据 processKey 获取最新版本的流程定义
     * GET /api/v1/process/definitions/key/{processKey}
     */
    @GetMapping("/key/{processKey}")
    public Result<ProcessDefinitionResponse> getByKey(@PathVariable String processKey) {
        ProcessDefinitionResponse response = definitionService.getByKey(processKey);
        return Result.ok(response);
    }

    /**
     * 更新流程定义
     * PUT /api/v1/process/definitions/{id}
     */
    @PutMapping("/{id}")
    public Result<ProcessDefinitionResponse> update(@PathVariable Long id,
                                                     @RequestBody ProcessDefinitionUpdateRequest request) {
        ProcessDefinitionResponse response = definitionService.update(id, request);
        return Result.ok(response);
    }

    /**
     * 删除流程定义
     * DELETE /api/v1/process/definitions/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        definitionService.delete(id);
        return Result.ok();
    }

    /**
     * 部署流程定义
     * POST /api/v1/process/definitions/{id}/deploy
     */
    @PostMapping("/{id}/deploy")
    public Result<ProcessDefinitionResponse> deploy(@PathVariable Long id) {
        ProcessDefinitionResponse response = definitionService.deploy(id);
        return Result.ok(response);
    }

    /**
     * 取消部署（已部署 → 草稿）
     * POST /api/v1/process/definitions/{id}/undeploy
     */
    @PostMapping("/{id}/undeploy")
    public Result<ProcessDefinitionResponse> undeploy(@PathVariable Long id) {
        ProcessDefinitionResponse response = definitionService.undeploy(id);
        return Result.ok(response);
    }

    /**
     * 导出流程定义
     * GET /api/v1/process/definitions/{id}/export
     */
    @GetMapping("/{id}/export")
    public Result<ProcessDefinitionResponse> export(@PathVariable Long id) {
        ProcessDefinitionResponse response = definitionService.export(id);
        return Result.ok(response);
    }

    /**
     * 导入流程定义
     * POST /api/v1/process/definitions/import
     */
    @PostMapping("/import")
    public Result<ProcessDefinitionResponse> importDefinition(@RequestBody ProcessDefinitionImportRequest request) {
        ProcessDefinitionResponse response = definitionService.importDefinition(request);
        return Result.ok(response);
    }
}
