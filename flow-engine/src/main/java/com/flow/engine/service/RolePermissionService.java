package com.flow.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.BusinessException;
import com.flow.engine.common.ErrorCode;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限管理服务（ISSUE-013）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final DataPermissionMapper dataPermissionMapper;

    // ========== 角色管理 ==========

    public Role createRole(Role role) {
        Role existing = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, role.getRoleKey()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.ROLE_KEY_DUPLICATE);
        }
        role.setStatus(1);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        log.info("创建角色: id={}, key={}", role.getId(), role.getRoleKey());
        return role;
    }

    public Role getRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return role;
    }

    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getSortOrder));
    }

    public Role updateRole(Long id, Role role) {
        Role existing = getRole(id);
        if (role.getRoleName() != null) existing.setRoleName(role.getRoleName());
        if (role.getRoleType() != null) existing.setRoleType(role.getRoleType());
        if (role.getSortOrder() != null) existing.setSortOrder(role.getSortOrder());
        if (role.getStatus() != null) existing.setStatus(role.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(existing);
        return existing;
    }

    public void deleteRole(Long id) {
        getRole(id);
        roleMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        dataPermissionMapper.delete(new LambdaQueryWrapper<DataPermission>().eq(DataPermission::getRoleId, id));
        log.info("删除角色: id={}", id);
    }

    // ========== 用户角色关联 ==========

    public void assignUserToRole(Long roleId, Long userId) {
        getRole(roleId);
        UserRole existing = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId).eq(UserRole::getUserId, userId));
        if (existing != null) return;
        UserRole userRole = new UserRole();
        userRole.setRoleId(roleId);
        userRole.setUserId(userId);
        userRoleMapper.insert(userRole);
    }

    public List<UserRole> getRoleUsers(Long roleId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
    }

    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return List.of();
        return roleMapper.selectBatchIds(roleIds);
    }

    // ========== 权限管理 ==========

    public Permission createPermission(Permission perm) {
        perm.setCreateTime(LocalDateTime.now());
        permissionMapper.insert(perm);
        return perm;
    }

    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortOrder));
    }

    public Permission updatePermission(Long id, Permission perm) {
        Permission existing = permissionMapper.selectById(id);
        if (existing == null) throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        if (perm.getPermName() != null) existing.setPermName(perm.getPermName());
        if (perm.getPermKey() != null) existing.setPermKey(perm.getPermKey());
        if (perm.getResourcePath() != null) existing.setResourcePath(perm.getResourcePath());
        permissionMapper.updateById(existing);
        return existing;
    }

    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getPermissionId, id));
    }

    // ========== 角色权限分配 ==========

    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        getRole(roleId);
        // 先清除旧权限
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        // 分配新权限
        for (Long permId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }
    }

    public List<Permission> getRolePermissions(Long roleId) {
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        List<Long> permIds = rps.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        if (permIds.isEmpty()) return List.of();
        return permissionMapper.selectBatchIds(permIds);
    }

    // ========== 数据权限 ==========

    public void setDataScope(Long roleId, Long deptId, Integer dataScope) {
        getRole(roleId);
        DataPermission existing = dataPermissionMapper.selectOne(
                new LambdaQueryWrapper<DataPermission>()
                        .eq(DataPermission::getRoleId, roleId)
                        .eq(DataPermission::getDeptId, deptId));
        if (existing != null) {
            existing.setDataScope(dataScope);
            dataPermissionMapper.updateById(existing);
        } else {
            DataPermission dp = new DataPermission();
            dp.setRoleId(roleId);
            dp.setDeptId(deptId);
            dp.setDataScope(dataScope);
            dataPermissionMapper.insert(dp);
        }
    }

    public List<DataPermission> getRoleDataPermissions(Long roleId) {
        return dataPermissionMapper.selectList(
                new LambdaQueryWrapper<DataPermission>().eq(DataPermission::getRoleId, roleId));
    }

    /**
     * 获取用户所有权限Key
     */
    public List<String> getUserPermissionKeys(Long userId) {
        List<Role> roles = getUserRoles(userId);
        if (roles.isEmpty()) return List.of();
        List<Long> roleIds = roles.stream().map(Role::getId).collect(Collectors.toList());

        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
        List<Long> permIds = rps.stream().map(RolePermission::getPermissionId).distinct().collect(Collectors.toList());
        if (permIds.isEmpty()) return List.of();

        List<Permission> perms = permissionMapper.selectBatchIds(permIds);
        return perms.stream().map(Permission::getPermKey).collect(Collectors.toList());
    }
}
