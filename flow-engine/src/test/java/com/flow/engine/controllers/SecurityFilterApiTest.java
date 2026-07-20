package com.flow.engine.controllers;

import com.flow.engine.common.utils.JsonUtils;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.Permission;
import com.flow.engine.entity.Role;
import com.flow.engine.entity.User;
import com.flow.engine.service.AuthService;
import com.flow.engine.service.DeptService;
import com.flow.engine.service.RolePermissionService;
import com.flow.engine.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 认证与权限API集成测试（ISSUE-013）
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("认证与权限API测试")
public class SecurityFilterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private DeptService deptService;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private AuthService authService;

    private String testUsername;
    private String testPassword = "testpwd123";

    @BeforeEach
    void setUp() {
        testUsername = "sec_api_user_" + System.currentTimeMillis();
        User user = new User();
        user.setUsername(testUsername);
        user.setPassword(AuthService.hashPassword(testPassword));
        user.setRealName("安全测试用户");
        user.setSecurityLevel(2);
        userService.createUser(user);
    }

    @Test
    @DisplayName("登录获取Token")
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(Map.of("username", testUsername, "password", testPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("错误密码登录失败")
    void testLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(Map.of("username", testUsername, "password", "wrongpwd"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("部门列表无需认证可访问")
    void testDeptListAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/system/depts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("用户CRUD完整流程")
    void testUserCrudFlow() throws Exception {
        // 获取用户列表
        mockMvc.perform(get("/api/v1/system/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 创建用户
        String newUsername = "crud_user_" + System.currentTimeMillis();
        User newUser = new User();
        newUser.setUsername(newUsername);
        newUser.setPassword("pwd");
        newUser.setRealName("CRUD测试");

        mockMvc.perform(post("/api/v1/system/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(newUsername));
    }

    @Test
    @DisplayName("角色权限分配流程")
    void testRolePermissionFlow() throws Exception {
        // 创建角色
        Role role = new Role();
        role.setRoleKey("api_test_role_" + System.currentTimeMillis());
        role.setRoleName("API测试角色");

        mockMvc.perform(post("/api/v1/system/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.roleKey").value(role.getRoleKey()));

        // 获取角色列表
        mockMvc.perform(get("/api/v1/system/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限CRUD流程")
    void testPermissionCrud() throws Exception {
        // 创建权限
        Permission perm = new Permission();
        perm.setPermName("API测试权限");
        perm.setPermKey("api:test:" + System.currentTimeMillis());
        perm.setPermType(3);

        mockMvc.perform(post("/api/v1/system/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(perm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.permKey").value(perm.getPermKey()));

        // 获取权限列表
        mockMvc.perform(get("/api/v1/system/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("部门树形接口")
    void testDeptTree() throws Exception {
        Dept parent = new Dept();
        parent.setDeptName("树形测试父_" + System.currentTimeMillis());
        parent = deptService.createDept(parent);

        Dept child = new Dept();
        child.setDeptName("树形测试子_" + System.currentTimeMillis());
        child.setParentId(parent.getId());
        deptService.createDept(child);

        mockMvc.perform(get("/api/v1/system/depts/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("用户兼职管理")
    void testUserPostManagement() throws Exception {
        User user = new User();
        user.setUsername("post_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user = userService.createUser(user);

        Dept dept = new Dept();
        dept.setDeptName("兼职测试部门_" + System.currentTimeMillis());
        dept = deptService.createDept(dept);

        // 添加兼职
        mockMvc.perform(post("/api/v1/system/users/" + user.getId() + "/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtils.toJson(Map.of("deptId", dept.getId(), "postId", 1L, "isMain", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 获取兼职
        mockMvc.perform(get("/api/v1/system/users/" + user.getId() + "/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
