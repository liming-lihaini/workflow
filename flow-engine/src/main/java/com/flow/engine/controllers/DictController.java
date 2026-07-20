package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.DictItem;
import com.flow.engine.entity.DictType;
import com.flow.engine.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典API（ISSUE-015, TRD §3.12）
 */
@RestController
@RequestMapping("/api/v1/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ========== 字典类型 ==========

    /**
     * 获取字典类型列表
     */
    @GetMapping("/types")
    public Result<List<DictType>> listTypes(
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictCode,
            @RequestParam(required = false) Integer dictType,
            @RequestParam(required = false) Integer status) {
        return Result.ok(dictService.getDictTypes(dictName, dictCode, dictType, status));
    }

    /**
     * 获取字典类型详情
     */
    @GetMapping("/types/{id}")
    public Result<DictType> getType(@PathVariable Long id) {
        return Result.ok(dictService.getDictType(id));
    }

    /**
     * 创建字典类型
     */
    @PostMapping("/types")
    public Result<DictType> createType(@RequestBody DictType dictType) {
        return Result.ok(dictService.createDictType(dictType));
    }

    /**
     * 更新字典类型
     */
    @PutMapping("/types/{id}")
    public Result<DictType> updateType(@PathVariable Long id, @RequestBody DictType dictType) {
        return Result.ok(dictService.updateDictType(id, dictType));
    }

    /**
     * 删除字典类型（系统内置不可删除）
     */
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return Result.ok();
    }

    // ========== 字典项 ==========

    /**
     * 获取字典项列表
     */
    @GetMapping("/items")
    public Result<List<DictItem>> listItems(
            @RequestParam(required = false) Long dictTypeId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(dictService.getDictItems(dictTypeId, status));
    }

    /**
     * 根据字典类型ID获取字典项
     */
    @GetMapping("/items/type/{typeId}")
    public Result<List<DictItem>> getItemsByTypeId(@PathVariable Long typeId) {
        return Result.ok(dictService.getDictItemsByTypeId(typeId));
    }

    /**
     * 根据字典编码获取字典项
     */
    @GetMapping("/items/code/{dictCode}")
    public Result<List<DictItem>> getItemsByCode(@PathVariable String dictCode) {
        return Result.ok(dictService.getDictItemsByCode(dictCode));
    }

    /**
     * 创建字典项
     */
    @PostMapping("/items")
    public Result<DictItem> createItem(@RequestBody DictItem dictItem) {
        return Result.ok(dictService.createDictItem(dictItem));
    }

    /**
     * 更新字典项
     */
    @PutMapping("/items/{id}")
    public Result<DictItem> updateItem(@PathVariable Long id, @RequestBody DictItem dictItem) {
        return Result.ok(dictService.updateDictItem(id, dictItem));
    }

    /**
     * 删除字典项
     */
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteDictItem(id);
        return Result.ok();
    }
}
