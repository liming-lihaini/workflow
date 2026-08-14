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
     * 按功能模块分组返回权限清单（三层结构：分组 -> 菜单子分组 -> 权限项）。
     * 二级分组以菜单类权限（permType=1）为子分组头，其按钮/数据权限按 parentId 归入；
     * 无法归属任何菜单的权限汇入“其他权限”子分组。
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

        // 组内权限索引：id -> 权限
        Map<Long, Permission> byId = new LinkedHashMap<>();
        for (Permission p : perms) {
            byId.put(p.getId(), p);
        }

        // 二级分组：菜单类权限作为子分组头，其余权限归入最近的菜单祖先下
        List<Permission> menus = perms.stream()
                .filter(p -> p.getPermType() != null && p.getPermType() == 1)
                .sorted(Comparator.comparing(Permission::getId))
                .toList();
        Map<Long, List<Permission>> childrenByMenu = new LinkedHashMap<>();
        for (Permission m : menus) {
            childrenByMenu.put(m.getId(), new ArrayList<>());
        }
        List<Permission> ungrouped = new ArrayList<>();
        for (Permission p : perms) {
            if (p.getPermType() != null && p.getPermType() == 1) {
                continue;
            }
            Long menuId = findMenuAncestor(p, byId);
            if (menuId != null) {
                childrenByMenu.get(menuId).add(p);
            } else {
                ungrouped.add(p);
            }
        }

        List<Map<String, Object>> subGroups = new ArrayList<>();
        for (Permission m : menus) {
            subGroups.add(buildSubGroupNode(m, childrenByMenu.get(m.getId())));
        }
        if (!ungrouped.isEmpty()) {
            subGroups.add(buildSubGroupNode(null, ungrouped));
        }
        groupNode.put("subGroups", subGroups);
        return groupNode;
    }

    /** 沿 parentId 向上查找最近的菜单类祖先（仅限组内） */
    private Long findMenuAncestor(Permission p, Map<Long, Permission> byId) {
        Long pid = p.getParentId();
        int guard = 0;
        while (pid != null && guard++ < 10) {
            Permission parent = byId.get(pid);
            if (parent == null) {
                return null;
            }
            if (parent.getPermType() != null && parent.getPermType() == 1) {
                return parent.getId();
            }
            pid = parent.getParentId();
        }
        return null;
    }

    /** 构建二级子分组节点（菜单自身也作为可勾选项列在首位） */
    private Map<String, Object> buildSubGroupNode(Permission menu, List<Permission> children) {
        Map<String, Object> node = new LinkedHashMap<>();
        if (menu != null) {
            node.put("key", "sub_" + menu.getId());
            node.put("id", menu.getId());
            node.put("title", menu.getPermName());
            node.put("permKey", menu.getPermKey());
        } else {
            node.put("key", "sub_other");
            node.put("title", "其他权限");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        if (menu != null) {
            items.add(buildPermNode(menu));
        }
        children.stream()
                .sorted(Comparator.comparing(Permission::getId))
                .forEach(c -> items.add(buildPermNode(c)));
        node.put("perms", items);
        return node;
    }

    private Map<String, Object> buildPermNode(Permission p) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("key", p.getId());
        node.put("id", p.getId());
        node.put("title", p.getPermName());
        node.put("permKey", p.getPermKey());
        node.put("permType", p.getPermType());
        node.put("permTypeLabel", TYPE_LABELS.getOrDefault(p.getPermType(), ""));
        return node;
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
