package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.*;
import com.flow.engine.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理API（ISSUE-013, TRD §3.9）
 */
@RestController
@RequestMapping("/api/v1/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(rolePermissionService.listRoles());
    }

    @GetMapping("/{id}")
    public Result<Role> get(@PathVariable Long id) {
        return Result.ok(rolePermissionService.getRole(id));
    }

    @PostMapping
    public Result<Role> create(@RequestBody Role role) {
        return Result.ok(rolePermissionService.createRole(role));
    }

    @PutMapping("/{id}")
    public Result<Role> update(@PathVariable Long id, @RequestBody Role role) {
        return Result.ok(rolePermissionService.updateRole(id, role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        rolePermissionService.deleteRole(id);
        return Result.ok();
    }

    @GetMapping("/{id}/users")
    public Result<List<UserRole>> getRoleUsers(@PathVariable Long id) {
        return Result.ok(rolePermissionService.getRoleUsers(id));
    }

    @PostMapping("/{id}/users")
    public Result<Void> assignUser(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        rolePermissionService.assignUserToRole(id, body.get("userId"));
        return Result.ok();
    }

    @GetMapping("/{id}/permissions")
    public Result<List<Permission>> getRolePermissions(@PathVariable Long id) {
        return Result.ok(rolePermissionService.getRolePermissions(id));
    }

    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        rolePermissionService.assignPermissions(id, permissionIds);
        return Result.ok();
    }

    @PutMapping("/{id}/data-scope")
    public Result<Void> setDataScope(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long deptId = Long.valueOf(body.get("deptId").toString());
        Integer dataScope = Integer.valueOf(body.get("dataScope").toString());
        rolePermissionService.setDataScope(id, deptId, dataScope);
        return Result.ok();
    }
}
