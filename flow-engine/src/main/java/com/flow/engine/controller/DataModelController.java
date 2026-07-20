package com.flow.engine.controller;

import com.flow.engine.common.Result;
import com.flow.engine.dto.*;
import com.flow.engine.service.DataModelService;
import com.flow.engine.service.ModelInstanceManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据模型控制器（ISSUE-010，TRD §3.5）
 */
@RestController
@RequestMapping("/api/v1/data-models")
@RequiredArgsConstructor
@CrossOrigin
public class DataModelController {

    private final DataModelService dataModelService;
    private final ModelInstanceManager modelInstanceManager;

    /**
     * 创建数据模型
     * POST /api/v1/data-models
     */
    @PostMapping
    public Result<DataModelResponse> create(@RequestBody DataModelRequest request) {
        DataModelResponse response = dataModelService.createModel(request);
        return Result.ok(response);
    }

    /**
     * 获取数据模型列表
     * GET /api/v1/data-models
     */
    @GetMapping
    public Result<List<DataModelResponse>> list() {
        List<DataModelResponse> list = dataModelService.listModels();
        return Result.ok(list);
    }

    /**
     * 获取数据模型详情
     * GET /api/v1/data-models/{modelKey}
     */
    @GetMapping("/{modelKey}")
    public Result<DataModelResponse> getByKey(@PathVariable String modelKey) {
        DataModelResponse response = dataModelService.getModel(modelKey);
        return Result.ok(response);
    }

    /**
     * 更新数据模型
     * PUT /api/v1/data-models/{modelKey}
     */
    @PutMapping("/{modelKey}")
    public Result<DataModelResponse> update(@PathVariable String modelKey,
                                            @RequestBody DataModelRequest request) {
        DataModelResponse response = dataModelService.updateModel(modelKey, request);
        return Result.ok(response);
    }

    /**
     * 删除数据模型
     * DELETE /api/v1/data-models/{modelKey}
     */
    @DeleteMapping("/{modelKey}")
    public Result<Void> delete(@PathVariable String modelKey) {
        dataModelService.deleteModel(modelKey);
        return Result.ok();
    }

    /**
     * 发布数据模型
     * POST /api/v1/data-models/{modelKey}/publish
     */
    @PostMapping("/{modelKey}/publish")
    public Result<DataModelResponse> publish(@PathVariable String modelKey) {
        DataModelResponse response = dataModelService.publishModel(modelKey);
        return Result.ok(response);
    }

    /**
     * 根据模型生成表单字段映射
     * GET /api/v1/data-models/{modelKey}/form-fields
     */
    @GetMapping("/{modelKey}/form-fields")
    public Result<List<DataModelService.FieldMapping>> generateFormFields(@PathVariable String modelKey) {
        List<DataModelService.FieldMapping> fields = dataModelService.generateFormFields(modelKey);
        return Result.ok(fields);
    }

    /**
     * 创建模型实例
     * POST /api/v1/data-models/{modelKey}/instances
     */
    @PostMapping("/{modelKey}/instances")
    public Result<ModelInstanceResponse> createInstance(@PathVariable String modelKey,
                                                         @RequestBody ModelInstanceRequest request) {
        request.setModelKey(modelKey);
        ModelInstanceResponse response = modelInstanceManager.createInstance(request);
        return Result.ok(response);
    }

    /**
     * 获取模型实例数据
     * GET /api/v1/data-models/instances/{instanceId}
     */
    @GetMapping("/instances/{instanceId}")
    public Result<ModelInstanceResponse> getInstance(@PathVariable String instanceId) {
        ModelInstanceResponse response = modelInstanceManager.getInstance(instanceId);
        return Result.ok(response);
    }

    /**
     * 更新模型实例数据
     * PUT /api/v1/data-models/instances/{instanceId}
     */
    @PutMapping("/instances/{instanceId}")
    public Result<ModelInstanceResponse> updateInstance(@PathVariable String instanceId,
                                                         @RequestBody Map<String, Object> data) {
        ModelInstanceResponse response = modelInstanceManager.updateInstance(instanceId, data);
        return Result.ok(response);
    }
}
