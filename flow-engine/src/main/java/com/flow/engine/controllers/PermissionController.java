package com.flow.engine.controllers;

import com.flow.engine.common.Result;
import com.flow.engine.entity.Permission;
import com.flow.engine.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理API（ISSUE-013, TRD §3.10）
 */
@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RolePermissionService rolePermissionService;

    private static final Map<String, String> GROUP_LABELS = new LinkedHashMap<>();
    static {
        GROUP_LABELS.put("dashboard", "工作台");
        GROUP_LABELS.put("process", "流程管理");
        GROUP_LABELS.put("task", "任务中心");
        GROUP_LABELS.put("system", "后台管理");
        GROUP_LABELS.put("monitor", "流程监控");
    }

    @GetMapping
    public Result<List<Permission>> list() {
        return Result.ok(rolePermissionService.listPermissions());
    }

    /**
     * 按功能模块分组返回权限树
     */
    @GetMapping("/grouped")
    public Result<List<Map<String, Object>>> listGrouped() {
        List<Permission> allPerms = rolePermissionService.listPermissions();
        // 按 permGroup 分组
        Map<String, List<Permission>> grouped = allPerms.stream()
                .filter(p -> p.getPermGroup() != null)
                .collect(Collectors.groupingBy(Permission::getPermGroup, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : GROUP_LABELS.entrySet()) {
            String groupKey = entry.getKey();
            String groupLabel = entry.getValue();
            List<Permission> perms = grouped.getOrDefault(groupKey, List.of());
            if (perms.isEmpty()) continue;

            Map<String, Object> groupNode = new LinkedHashMap<>();
            groupNode.put("key", "group_" + groupKey);
            groupNode.put("title", groupLabel);
            groupNode.put("permGroup", groupKey);
            groupNode.put("selectable", false);
            // 构建树形子节点
            groupNode.put("children", buildTree(perms));
            result.add(groupNode);
        }
        return Result.ok(result);
    }

    /**
     * 将平铺的权限列表构建为树形结构（基于 parentId）
     */
    private List<Map<String, Object>> buildTree(List<Permission> perms) {
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        for (Permission p : perms) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("key", p.getId());
            node.put("title", p.getPermName());
            node.put("id", p.getId());
            node.put("permKey", p.getPermKey());
            node.put("permType", p.getPermType());
            node.put("children", new ArrayList<>());
            nodeMap.put(p.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Permission p : perms) {
            Map<String, Object> node = nodeMap.get(p.getId());
            if (p.getParentId() == null || p.getParentId() == 0 || !nodeMap.containsKey(p.getParentId())) {
                roots.add(node);
            } else {
                Map<String, Object> parent = nodeMap.get(p.getParentId());
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(node);
            }
        }
        return roots;
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
