package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.Permission;
import com.flow.engine.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理API（ISSUE-013, TRD §3.10）
 */
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    public Result<List<Permission>> list() {
        return Result.ok(rolePermissionService.listPermissions());
    }

    @PostMapping
    public Result<Permission> create(@RequestBody Permission perm) {
        return Result.ok(rolePermissionService.createPermission(perm));
    }

    @PutMapping("/{id}")
    public Result<Permission> update(@PathVariable Long id, @RequestBody Permission perm) {
        return Result.ok(rolePermissionService.updatePermission(id, perm));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rolePermissionService.deletePermission(id);
        return Result.ok();
    }
}
