package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 部门服务测试（ISSUE-013）
 */
@SpringBootTest
@DisplayName("部门服务测试")
public class DeptServiceTest {

    @Autowired
    private DeptService deptService;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("创建部门")
    void testCreateDept() {
        Dept dept = new Dept();
        dept.setDeptName("测试部门" + System.currentTimeMillis());
        dept.setDeptCode("TEST_" + System.currentTimeMillis());
        Dept created = deptService.createDept(dept);
        assertNotNull(created.getId());
        assertEquals(1, created.getStatus());
    }

    @Test
    @DisplayName("部门树形结构")
    void testDeptTree() {
        Dept parent = new Dept();
        parent.setDeptName("父部门_" + System.currentTimeMillis());
        parent = deptService.createDept(parent);

        Dept child = new Dept();
        child.setDeptName("子部门_" + System.currentTimeMillis());
        child.setParentId(parent.getId());
        deptService.createDept(child);

        List<Map<String, Object>> tree = deptService.getDeptTree();
        assertFalse(tree.isEmpty());
    }

    @Test
    @DisplayName("设置部门领导")
    void testSetLeader() {
        Dept dept = new Dept();
        dept.setDeptName("领导测试部门_" + System.currentTimeMillis());
        dept = deptService.createDept(dept);

        Dept updated = deptService.setLeader(dept.getId(), 1L, "张三");
        assertEquals(1L, updated.getLeaderId());
        assertEquals("张三", updated.getLeaderName());
    }

    @Test
    @DisplayName("删除有子部门的部门失败")
    void testDeleteDeptWithChildren() {
        Dept parent = new Dept();
        parent.setDeptName("父部门_" + System.currentTimeMillis());
        final Dept createdParent = deptService.createDept(parent);

        Dept child = new Dept();
        child.setDeptName("子部门_" + System.currentTimeMillis());
        child.setParentId(createdParent.getId());
        deptService.createDept(child);

        assertThrows(BusinessException.class, () -> deptService.deleteDept(createdParent.getId()));
    }

    @Test
    @DisplayName("删除有用户的部门失败")
    void testDeleteDeptWithUsers() {
        Dept dept = new Dept();
        dept.setDeptName("用户部门_" + System.currentTimeMillis());
        final Dept createdDept = deptService.createDept(dept);

        User user = new User();
        user.setUsername("testuser_" + System.currentTimeMillis());
        user.setPassword("testpwd");
        user.setDeptId(createdDept.getId());
        userService.createUser(user);

        assertThrows(BusinessException.class, () -> deptService.deleteDept(createdDept.getId()));
    }

    @Test
    @DisplayName("获取子部门ID列表")
    void testGetChildDeptIds() {
        Dept parent = new Dept();
        parent.setDeptName("父_" + System.currentTimeMillis());
        parent = deptService.createDept(parent);

        Dept child1 = new Dept();
        child1.setDeptName("子1_" + System.currentTimeMillis());
        child1.setParentId(parent.getId());
        child1 = deptService.createDept(child1);

        Dept child2 = new Dept();
        child2.setDeptName("子2_" + System.currentTimeMillis());
        child2.setParentId(parent.getId());
        deptService.createDept(child2);

        List<Long> childIds = deptService.getChildDeptIds(parent.getId());
        assertTrue(childIds.size() >= 2);
    }
}
