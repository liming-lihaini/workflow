package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限决策器测试（ISSUE-013）
 */
@SpringBootTest
@DisplayName("权限决策器测试")
public class PermissionEvaluatorTest {

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private DeptService deptService;

    @Test
    @DisplayName("有权限用户返回true")
    void testHasPermission() {
        // 创建权限
        Permission perm = new Permission();
        perm.setPermName("测试权限");
        perm.setPermKey("test:perm:" + System.currentTimeMillis());
        perm = rolePermissionService.createPermission(perm);

        // 创建角色
        Role role = new Role();
        role.setRoleKey("test_role_" + System.currentTimeMillis());
        role.setRoleName("测试角色");
        role = rolePermissionService.createRole(role);

        // 分配权限
        rolePermissionService.assignPermissions(role.getId(), List.of(perm.getId()));

        // 创建用户
        User user = new User();
        user.setUsername("perm_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user = userService.createUser(user);

        // 分配角色
        rolePermissionService.assignUserToRole(role.getId(), user.getId());

        assertTrue(permissionEvaluator.hasPermission(user.getId(), perm.getPermKey()));
    }

    @Test
    @DisplayName("无权限用户返回false")
    void testNoPermission() {
        User user = new User();
        user.setUsername("no_perm_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user = userService.createUser(user);

        assertFalse(permissionEvaluator.hasPermission(user.getId(), "nonexistent:perm"));
    }

    @Test
    @DisplayName("密级校验通过")
    void testSecurityLevelPass() {
        User user = new User();
        user.setUsername("sec_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user.setSecurityLevel(3);
        user = userService.createUser(user);

        assertTrue(permissionEvaluator.checkSecurityLevel(user.getId(), 2));
        assertTrue(permissionEvaluator.checkSecurityLevel(user.getId(), 3));
    }

    @Test
    @DisplayName("密级校验拒绝")
    void testSecurityLevelDenied() {
        User user = new User();
        user.setUsername("low_sec_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user.setSecurityLevel(1);
        final User created = userService.createUser(user);

        assertFalse(permissionEvaluator.checkSecurityLevel(created.getId(), 3));
        assertThrows(BusinessException.class, () -> permissionEvaluator.checkSecurityLevelDenied(created.getId(), 3));
    }

    @Test
    @DisplayName("数据权限-全部范围")
    void testDataScopeAll() {
        Dept dept = new Dept();
        dept.setDeptName("数据权限部门_" + System.currentTimeMillis());
        dept = deptService.createDept(dept);

        Role role = new Role();
        role.setRoleKey("data_role_" + System.currentTimeMillis());
        role.setRoleName("数据权限角色");
        role = rolePermissionService.createRole(role);

        // 设置全部数据权限
        rolePermissionService.setDataScope(role.getId(), dept.getId(), 1);

        User user = new User();
        user.setUsername("data_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user.setDeptId(dept.getId());
        user = userService.createUser(user);

        rolePermissionService.assignUserToRole(role.getId(), user.getId());

        List<Long> accessibleDepts = permissionEvaluator.getAccessibleDepts(user.getId());
        assertFalse(accessibleDepts.isEmpty());
    }
}
