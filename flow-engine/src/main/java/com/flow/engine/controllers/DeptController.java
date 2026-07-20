package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.*;
import com.flow.engine.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 部门管理API（ISSUE-013, TRD §3.7）
 */
@RestController
@RequestMapping("/api/v1/system/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping
    public Result<List<Dept>> list() {
        return Result.ok(deptService.listDepts());
    }

    /**
     * 分页查询部门（支持条件搜索）
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> listPage(
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(deptService.listDeptsPage(deptName, status, page, size));
    }

    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree() {
        return Result.ok(deptService.getDeptTree());
    }

    @GetMapping("/{id}")
    public Result<Dept> get(@PathVariable Long id) {
        return Result.ok(deptService.getDept(id));
    }

    @PostMapping
    public Result<Dept> create(@RequestBody Dept dept) {
        return Result.ok(deptService.createDept(dept));
    }

    @PutMapping("/{id}")
    public Result<Dept> update(@PathVariable Long id, @RequestBody Dept dept) {
        return Result.ok(deptService.updateDept(id, dept));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.ok();
    }

    @PutMapping("/{id}/leader")
    public Result<Dept> setLeader(@PathVariable Long id, @RequestParam Long leaderId, @RequestParam String leaderName) {
        return Result.ok(deptService.setLeader(id, leaderId, leaderName));
    }
}
