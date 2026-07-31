package com.flow.engine.controller;

import com.flow.engine.annotation.OpLog;
import com.flow.engine.common.Result;
import com.flow.engine.service.ModelDataService;
import com.flow.engine.service.ModelMenuPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型业务数据控制器
 * <p>
 * 面向数据模型生成的物理表提供通用数据管理接口：
 * 动态菜单、关键字检索、分页列表、详情、新增、修改、删除。
 */
@RestController
@RequestMapping("/api/v1/model-data")
@RequiredArgsConstructor
@CrossOrigin
public class ModelDataController {

    private final ModelDataService modelDataService;
    private final ModelMenuPermissionService modelMenuPermissionService;

    /**
     * 查询模型数据动态菜单
     * GET /api/v1/model-data/menus
     */
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> menus() {
        return Result.ok(modelMenuPermissionService.listModelMenus());
    }

    /**
     * 分页查询模型数据（支持关键字检索）
     * GET /api/v1/model-data/{modelKey}?keyword=&page=&size=
     */
    @GetMapping("/{modelKey}")
    public Result<Map<String, Object>> page(@PathVariable String modelKey,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(modelDataService.page(modelKey, keyword, page, size));
    }

    /**
     * 查询详情（含子表数据）
     * GET /api/v1/model-data/{modelKey}/{id}
     */
    @GetMapping("/{modelKey}/{id}")
    public Result<Map<String, Object>> detail(@PathVariable String modelKey, @PathVariable Long id) {
        return Result.ok(modelDataService.detail(modelKey, id));
    }

    /**
     * 新增数据
     * POST /api/v1/model-data/{modelKey}
     */
    @PostMapping("/{modelKey}")
    @OpLog(module = "业务数据", operation = "新增模型数据")
    public Result<Map<String, Object>> create(@PathVariable String modelKey,
                                              @RequestBody Map<String, Object> data) {
        return Result.ok(modelDataService.create(modelKey, data));
    }

    /**
     * 修改数据
     * PUT /api/v1/model-data/{modelKey}/{id}
     */
    @PutMapping("/{modelKey}/{id}")
    @OpLog(module = "业务数据", operation = "修改模型数据")
    public Result<Void> update(@PathVariable String modelKey, @PathVariable Long id,
                               @RequestBody Map<String, Object> data) {
        modelDataService.update(modelKey, id, data);
        return Result.ok();
    }

    /**
     * 删除数据
     * DELETE /api/v1/model-data/{modelKey}/{id}
     */
    @DeleteMapping("/{modelKey}/{id}")
    @OpLog(module = "业务数据", operation = "删除模型数据")
    public Result<Void> delete(@PathVariable String modelKey, @PathVariable Long id) {
        modelDataService.delete(modelKey, id);
        return Result.ok();
    }
}
