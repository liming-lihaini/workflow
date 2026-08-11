package com.flow.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.Permission;
import com.flow.engine.entity.Role;
import com.flow.engine.entity.RolePermission;
import com.flow.engine.mapper.PermissionMapper;
import com.flow.engine.mapper.RoleMapper;
import com.flow.engine.mapper.RolePermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 合同管理台账权限种子（PRD-02）：幂等创建 ems:contract 菜单及
 * edit/finance/delete 按钮权限，并分配给 system_admin 角色。
 * 其余 EMS 权限为数据库存量维护，此处仅补齐合同台账新增项。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(13)
public class EmsContractPermissionInitializer implements CommandLineRunner {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;

    @Override
    public void run(String... args) {
        Long emsMenuId = findPermId("ems");
        Long parentId = emsMenuId != null ? emsMenuId : 0L;

        Long contractMenuId = createPermissionIfAbsent(parentId, "合同台账", "ems:contract", 1, "ems", "/ems/contract", 30);
        Long editId = createPermissionIfAbsent(contractMenuId, "合同新建/编辑/状态操作", "ems:contract:edit", 2, "ems", null, 1);
        Long financeId = createPermissionIfAbsent(contractMenuId, "收款/支付登记与撤销", "ems:contract:finance", 2, "ems", null, 2);
        Long deleteId = createPermissionIfAbsent(contractMenuId, "删除草稿合同/收付款登记", "ems:contract:delete", 2, "ems", null, 3);

        Role sysAdmin = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, "system_admin"));
        if (sysAdmin != null) {
            for (Long permId : new Long[]{contractMenuId, editId, financeId, deleteId}) {
                assignIfAbsent(sysAdmin.getId(), permId);
            }
        }
        log.info("[EmsContractPermissionInitializer] 合同台账权限种子初始化完成");
    }

    private Long findPermId(String permKey) {
        Permission perm = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, permKey));
        return perm != null ? perm.getId() : null;
    }

    private Long createPermissionIfAbsent(Long parentId, String permName, String permKey,
                                          Integer permType, String permGroup, String resourcePath, Integer sortOrder) {
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
        return perm.getId();
    }

    private void assignIfAbsent(Long roleId, Long permissionId) {
        Long count = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId)
                        .eq(RolePermission::getPermissionId, permissionId));
        if (count != null && count > 0) {
            return;
        }
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionMapper.insert(rp);
    }
}
