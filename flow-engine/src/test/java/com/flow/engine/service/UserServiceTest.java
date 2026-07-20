package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.User;
import com.flow.engine.entity.UserPost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试（ISSUE-013）
 */
@SpringBootTest
@DisplayName("用户服务测试")
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DeptService deptService;

    @Test
    @DisplayName("创建用户")
    void testCreateUser() {
        User user = new User();
        user.setUsername("user_" + System.currentTimeMillis());
        user.setPassword("pwd123");
        user.setRealName("测试用户");
        User created = userService.createUser(user);
        assertNotNull(created.getId());
        assertEquals(1, created.getSecurityLevel());
    }

    @Test
    @DisplayName("用户名重复")
    void testDuplicateUsername() {
        String username = "dup_user_" + System.currentTimeMillis();
        User user1 = new User();
        user1.setUsername(username);
        user1.setPassword("pwd");
        userService.createUser(user1);

        User user2 = new User();
        user2.setUsername(username);
        user2.setPassword("pwd");
        assertThrows(BusinessException.class, () -> userService.createUser(user2));
    }

    @Test
    @DisplayName("用户兼职多部门")
    void testUserMultiDept() {
        Dept dept1 = new Dept();
        dept1.setDeptName("兼职部门1_" + System.currentTimeMillis());
        dept1 = deptService.createDept(dept1);

        Dept dept2 = new Dept();
        dept2.setDeptName("兼职部门2_" + System.currentTimeMillis());
        dept2 = deptService.createDept(dept2);

        User user = new User();
        user.setUsername("multi_dept_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user.setDeptId(dept1.getId());
        user = userService.createUser(user);

        // 添加兼职
        userService.addUserPost(user.getId(), dept2.getId(), 1L, false);

        List<UserPost> posts = userService.getUserPosts(user.getId());
        assertTrue(posts.size() >= 2);

        List<Long> deptIds = userService.getUserDeptIds(user.getId());
        assertTrue(deptIds.contains(dept1.getId()));
        assertTrue(deptIds.contains(dept2.getId()));
    }

    @Test
    @DisplayName("获取用户可访问部门含兼职")
    void testAccessibleDepts() {
        Dept dept1 = new Dept();
        dept1.setDeptName("可访问部门1_" + System.currentTimeMillis());
        dept1 = deptService.createDept(dept1);

        Dept dept2 = new Dept();
        dept2.setDeptName("可访问部门2_" + System.currentTimeMillis());
        dept2 = deptService.createDept(dept2);

        User user = new User();
        user.setUsername("access_dept_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user.setDeptId(dept1.getId());
        user = userService.createUser(user);

        userService.addUserPost(user.getId(), dept2.getId(), 1L, false);

        List<Long> accessibleDepts = userService.getAccessibleDepts(user.getId());
        assertTrue(accessibleDepts.contains(dept1.getId()));
        assertTrue(accessibleDepts.contains(dept2.getId()));
    }

    @Test
    @DisplayName("设置密级")
    void testSetSecurityLevel() {
        User user = new User();
        user.setUsername("sec_user_" + System.currentTimeMillis());
        user.setPassword("pwd");
        user = userService.createUser(user);

        userService.setSecurityLevel(user.getId(), 3);
        User updated = userService.getUser(user.getId());
        assertEquals(3, updated.getSecurityLevel());
    }
}
