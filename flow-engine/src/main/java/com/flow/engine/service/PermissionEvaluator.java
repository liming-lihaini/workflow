package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.DataPermission;
import com.flow.engine.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限决策器（ISSUE-013）
 * <p>
 * 提供功能权限、数据权限、密级校验的统一决策入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEvaluator {

    private final RolePermissionService rolePermissionService;
    private final UserService userService;
    private final DeptService deptService;

    /**
     * 判断用户是否有指定功能权限
     */
    public boolean hasPermission(Long userId, String permKey) {
        List<String> permKeys = rolePermissionService.getUserPermissionKeys(userId);
        return permKeys.contains(permKey);
    }

    /**
     * 校验功能权限，无权限则抛异常
     */
    public void checkPermission(Long userId, String permKey) {
        if (!hasPermission(userId, permKey)) {
            throw new BusinessException(com.flow.engine.common.ErrorCode.PERMISSION_DENIED);
        }
    }

    /**
     * 判断用户是否可以访问指定部门的数据
     */
    public boolean canAccessData(Long userId, Long deptId, String dataType) {
        User user = userService.getUser(userId);
        List<Long> userDepts = userService.getAccessibleDepts(userId);

        // 用户直接所属的部门可访问
        if (userDepts.contains(deptId)) {
            return true;
        }

        // 检查数据权限范围
        List<com.flow.engine.entity.Role> roles = rolePermissionService.getUserRoles(userId);
        for (com.flow.engine.entity.Role role : roles) {
            List<DataPermission> dps = rolePermissionService.getRoleDataPermissions(role.getId());
            for (DataPermission dp : dps) {
                switch (dp.getDataScope()) {
                    case 1: // 全部
                        return true;
                    case 2: // 本部门
                        if (userDepts.contains(deptId)) return true;
                        break;
                    case 3: // 本部门及子部门
                        if (userDepts.contains(deptId)) return true;
                        List<Long> childDepts = deptService.getChildDeptIds(deptId);
                        if (childDepts.stream().anyMatch(userDepts::contains)) return true;
                        break;
                    case 4: // 仅本人
                        return true; // 本人数据总是可访问
                }
            }
        }
        return false;
    }

    /**
     * 获取用户可访问的部门列表
     */
    public List<Long> getAccessibleDepts(Long userId) {
        User user = userService.getUser(userId);
        List<Long> userDepts = userService.getAccessibleDepts(userId);

        // 检查是否有全部数据权限
        List<com.flow.engine.entity.Role> roles = rolePermissionService.getUserRoles(userId);
        for (com.flow.engine.entity.Role role : roles) {
            List<DataPermission> dps = rolePermissionService.getRoleDataPermissions(role.getId());
            for (DataPermission dp : dps) {
                if (dp.getDataScope() == 1) {
                    // 全部数据权限，返回所有部门
                    return deptService.listDepts().stream()
                            .map(com.flow.engine.entity.Dept::getId)
                            .toList();
                }
            }
        }
        return userDepts;
    }

    /**
     * 校验用户密级是否满足要求
     */
    public boolean checkSecurityLevel(Long userId, Integer requiredLevel) {
        User user = userService.getUser(userId);
        Integer userLevel = user.getSecurityLevel();
        if (userLevel == null) userLevel = 1;
        return userLevel >= requiredLevel;
    }

    /**
     * 校验密级，不足则抛异常
     */
    public void checkSecurityLevelDenied(Long userId, Integer requiredLevel) {
        if (!checkSecurityLevel(userId, requiredLevel)) {
            throw new BusinessException(com.flow.engine.common.ErrorCode.SECURITY_LEVEL_DENIED);
        }
    }
}
