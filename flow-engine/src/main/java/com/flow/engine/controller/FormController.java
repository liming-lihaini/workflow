package com.flow.engine.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.flow.engine.common.Result;
import com.flow.engine.entity.FormDefinition;
import com.flow.engine.service.FormDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表单控制器（ISSUE-008，TRD §3.4）
 */
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
@CrossOrigin
public class FormController {

    private final FormDefinitionService formDefinitionService;

    /**
     * 获取表单定义列表（分页）
     * GET /api/v1/forms?page=1&size=10&keyword=
     */
    @GetMapping
    public Result<IPage<FormDefinition>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        IPage<FormDefinition> list = formDefinitionService.getFormList(page, size, keyword);
        return Result.ok(list);
    }

    /**
     * 获取所有表单定义（下拉选择用）
     * GET /api/v1/forms/all
     */
    @GetMapping("/all")
    public Result<List<FormDefinition>> listAll() {
        List<FormDefinition> list = formDefinitionService.getFormList();
        return Result.ok(list);
    }

    /**
     * 获取表单定义详情
     * GET /api/v1/forms/{formKey}
     */
    @GetMapping("/{formKey}")
    public Result<FormDefinition> get(@PathVariable String formKey) {
        FormDefinition form = formDefinitionService.getForm(formKey);
        return Result.ok(form);
    }

    /**
     * 创建表单定义
     * POST /api/v1/forms
     */
    @PostMapping
    public Result<FormDefinition> create(@RequestBody FormDefinition form) {
        FormDefinition created = formDefinitionService.createForm(form);
        return Result.ok(created);
    }

    /**
     * 更新表单定义
     * PUT /api/v1/forms/{formKey}
     */
    @PutMapping("/{formKey}")
    public Result<FormDefinition> update(@PathVariable String formKey, @RequestBody FormDefinition form) {
        FormDefinition updated = formDefinitionService.updateForm(formKey, form);
        return Result.ok(updated);
    }

    /**
     * 删除表单定义
     * DELETE /api/v1/forms/{formKey}
     */
    @DeleteMapping("/{formKey}")
    public Result<Void> delete(@PathVariable String formKey) {
        formDefinitionService.deleteForm(formKey);
        return Result.ok();
    }
}
