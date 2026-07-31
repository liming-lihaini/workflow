package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.Permission;
import com.flow.engine.entity.Role;
import com.flow.engine.entity.RolePermission;
import com.flow.engine.mapper.PermissionMapper;
import com.flow.engine.mapper.RoleMapper;
import com.flow.engine.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型菜单与权限同步服务
 * <p>
 * 数据模型生成数据库表时，同步创建「业务数据」菜单及模型子菜单、按钮权限，
 * 并绑定到 system_admin 角色，使权限体系与生成的数据表保持一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelMenuPermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;

    /** 业务数据根菜单权限 Key */
    public static final String ROOT_PERM_KEY = "model-data";

    /**
     * 同步模型对应的菜单与按钮权限（幂等）
     *
     * @return 本次新建的权限 Key 列表
     */
    @Transactional
    public List<String> syncMenuAndPermissions(String modelKey, String modelName) {
        List<String> created = new ArrayList<>();

        // 根菜单：业务数据
        Long rootId = createIfAbsent(0L, "业务数据", ROOT_PERM_KEY, 1, "model", "/model-data", 90, created);

        // 模型菜单
        String menuKey = ROOT_PERM_KEY + ":" + modelKey;
        Long menuId = createIfAbsent(rootId, modelName, menuKey, 1, "model", "/model-data/" + modelKey, nextSortOrder(rootId), created);

        // 按钮权限
        createIfAbsent(menuId, "查看" + modelName, menuKey + ":view", 2, "model", null, 1, created);
        createIfAbsent(menuId, "新增" + modelName, menuKey + ":create", 2, "model", null, 2, created);
        createIfAbsent(menuId, "编辑" + modelName, menuKey + ":update", 2, "model", null, 3, created);
        createIfAbsent(menuId, "删除" + modelName, menuKey + ":delete", 2, "model", null, 4, created);

        // 绑定 system_admin 角色
        assignToRole("system_admin", ROOT_PERM_KEY, menuKey,
                menuKey + ":view", menuKey + ":create", menuKey + ":update", menuKey + ":delete");

        if (!created.isEmpty()) {
            log.info("[ModelMenuPermissionService] 同步模型权限: modelKey={}, created={}", modelKey, created);
        }
        return created;
    }

    /**
     * 查询所有模型数据菜单（供前端动态渲染侧边栏）
     */
    public List<Map<String, Object>> listModelMenus() {
        List<Permission> menus = permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermType, 1)
                .likeRight(Permission::getPermKey, ROOT_PERM_KEY + ":")
                .orderByAsc(Permission::getSortOrder));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Permission menu : menus) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("permKey", menu.getPermKey());
            item.put("permName", menu.getPermName());
            item.put("resourcePath", menu.getResourcePath());
            item.put("modelKey", menu.getPermKey().substring((ROOT_PERM_KEY + ":").length()));
            result.add(item);
        }
        return result;
    }

    /** 按 permKey 幂等创建权限，新建时记录到 created 列表 */
    private Long createIfAbsent(Long parentId, String permName, String permKey, Integer permType,
                                String permGroup, String resourcePath, Integer sortOrder, List<String> created) {
        Permission existing = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, permKey));
        if (existing != null) {
            return existing.getId();
        }
        Permission perm = new Permission();
        perm.setParentId(parentId);
        perm.setPermName(permName);
        perm.setPermKey(permKey);
        perm.setPermType(permType);
        perm.setPermGroup(permGroup);
        perm.setResourcePath(resourcePath);
        perm.setSortOrder(sortOrder);
        perm.setCreateTime(LocalDateTime.now());
        permissionMapper.insert(perm);
        created.add(permKey);
        return perm.getId();
    }

    /** 同级菜单下一个排序号 */
    private int nextSortOrder(Long parentId) {
        Long count = permissionMapper.selectCount(
                new LambdaQueryWrapper<Permission>().eq(Permission::getParentId, parentId));
        return count == null ? 1 : count.intValue() + 1;
    }

    /** 将指定权限绑定到角色（查重后插入） */
    private void assignToRole(String roleKey, String... permKeys) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, roleKey));
        if (role == null) {
            log.warn("[ModelMenuPermissionService] 角色不存在，跳过权限绑定: roleKey={}", roleKey);
            return;
        }
        for (String permKey : permKeys) {
            Permission perm = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, permKey));
            if (perm == null) {
                continue;
            }
            Long exists = rolePermissionMapper.selectCount(new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, role.getId())
                    .eq(RolePermission::getPermissionId, perm.getId()));
            if (exists == null || exists == 0) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(perm.getId());
                rolePermissionMapper.insert(rp);
            }
        }
    }
}
