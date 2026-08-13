package com.flow.engine.controllers;

import com.flow.engine.annotation.OpLog;
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
        GROUP_LABELS.put("ems", "环境检测");
        GROUP_LABELS.put("monitor", "流程监控");
        GROUP_LABELS.put("model", "业务数据");
    }

    private static final Map<Integer, String> TYPE_LABELS = Map.of(
            1, "菜单", 2, "按钮", 3, "数据权限"
    );

    @GetMapping
    public Result<List<Permission>> list() {
        return Result.ok(rolePermissionService.listPermissions());
    }

    /**
     * 按功能模块分组返回权限清单（扁平化两层结构：分组 -> 权限项平铺）
     */
    @GetMapping("/grouped")
    public Result<List<Map<String, Object>>> listGrouped() {
        List<Permission> allPerms = rolePermissionService.listPermissions();
        // 按 permGroup 分组
        Map<String, List<Permission>> grouped = allPerms.stream()
                .filter(p -> p.getPermGroup() != null)
                .collect(Collectors.groupingBy(Permission::getPermGroup, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> emitted = new LinkedHashSet<>();
        for (Map.Entry<String, String> entry : GROUP_LABELS.entrySet()) {
            String groupKey = entry.getKey();
            List<Permission> perms = grouped.getOrDefault(groupKey, List.of());
            if (perms.isEmpty()) continue;
            result.add(buildGroupNode(groupKey, entry.getValue(), perms));
            emitted.add(groupKey);
        }
        // 未在 GROUP_LABELS 中登记的分组（如动态产生的模型分组）也予以展示
        for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
            if (emitted.contains(entry.getKey()) || entry.getValue().isEmpty()) continue;
            result.add(buildGroupNode(entry.getKey(), entry.getKey(), entry.getValue()));
        }
        return Result.ok(result);
    }

    /** 构建分组节点：子节点为组内权限项平铺，不再按 parentId 嵌套 */
    private Map<String, Object> buildGroupNode(String groupKey, String groupLabel, List<Permission> perms) {
        Map<String, Object> groupNode = new LinkedHashMap<>();
        groupNode.put("key", "group_" + groupKey);
        groupNode.put("title", groupLabel);
        groupNode.put("permGroup", groupKey);
        groupNode.put("selectable", false);

        List<Map<String, Object>> children = new ArrayList<>();
        List<Permission> sorted = perms.stream()
                .sorted(Comparator.comparing(Permission::getId))
                .toList();
        for (Permission p : sorted) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("key", p.getId());
            node.put("title", p.getPermName());
            node.put("id", p.getId());
            node.put("permKey", p.getPermKey());
            node.put("permType", p.getPermType());
            node.put("permTypeLabel", TYPE_LABELS.getOrDefault(p.getPermType(), ""));
            children.add(node);
        }
        groupNode.put("children", children);
        return groupNode;
    }

    @PostMapping
    @OpLog(module = "权限管理", operation = "创建权限")
    public Result<Permission> create(@RequestBody Permission perm) {
        return Result.ok(rolePermissionService.createPermission(perm));
    }

    @PutMapping("/{id}")
    @OpLog(module = "权限管理", operation = "更新权限")
    public Result<Permission> update(@PathVariable Long id, @RequestBody Permission perm) {
        return Result.ok(rolePermissionService.updatePermission(id, perm));
    }

    @DeleteMapping("/{id}")
    @OpLog(module = "权限管理", operation = "删除权限")
    public Result<Void> delete(@PathVariable Long id) {
        rolePermissionService.deletePermission(id);
        return Result.ok();
    }
}
