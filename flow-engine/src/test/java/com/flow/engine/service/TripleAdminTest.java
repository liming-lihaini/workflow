package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.enums.AdminType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三员管理测试（ISSUE-016）
 */
@SpringBootTest
@DisplayName("三员管理测试")
public class TripleAdminTest {

    @Autowired
    private TripleAdminService tripleAdminService;

    @Autowired
    private TripleAdminPermissionEvaluator permissionEvaluator;

    @Test
    @DisplayName("获取管理员类型列表")
    void testGetAdminTypes() {
        List<Map<String, Object>> types = tripleAdminService.getAdminTypes();
        assertEquals(3, types.size());
    }

    @Test
    @DisplayName("三员账号初始化成功")
    void testTripleAdminsInitialized() {
        // 验证三员角色已创建
        List<Map<String, Object>> users = tripleAdminService.getTripleAdminUsers(null);
        assertTrue(users.size() >= 3, "三员账号应至少有3个");
    }

    @Test
    @DisplayName("判断用户是否为系统管理员")
    void testIsSystemAdmin() {
        // 获取系统管理员用户
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        assertFalse(sysAdmins.isEmpty());
        
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        assertTrue(tripleAdminService.isSystemAdmin(sysAdminId));
        assertFalse(tripleAdminService.isSecurityAdmin(sysAdminId));
        assertFalse(tripleAdminService.isAuditAdmin(sysAdminId));
    }

    @Test
    @DisplayName("判断用户是否为安全管理员")
    void testIsSecurityAdmin() {
        List<Map<String, Object>> secAdmins = tripleAdminService.getTripleAdminUsers("2");
        assertFalse(secAdmins.isEmpty());
        
        Long secAdminId = (Long) secAdmins.get(0).get("id");
        assertFalse(tripleAdminService.isSystemAdmin(secAdminId));
        assertTrue(tripleAdminService.isSecurityAdmin(secAdminId));
        assertFalse(tripleAdminService.isAuditAdmin(secAdminId));
    }

    @Test
    @DisplayName("判断用户是否为审计管理员")
    void testIsAuditAdmin() {
        List<Map<String, Object>> auditAdmins = tripleAdminService.getTripleAdminUsers("3");
        assertFalse(auditAdmins.isEmpty());
        
        Long auditAdminId = (Long) auditAdmins.get(0).get("id");
        assertFalse(tripleAdminService.isSystemAdmin(auditAdminId));
        assertFalse(tripleAdminService.isSecurityAdmin(auditAdminId));
        assertTrue(tripleAdminService.isAuditAdmin(auditAdminId));
    }

    @Test
    @DisplayName("三员之间不能互相操作")
    void testTripleAdminCannotOperateEachOther() {
        // 获取三员ID
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        List<Map<String, Object>> secAdmins = tripleAdminService.getTripleAdminUsers("2");
        List<Map<String, Object>> auditAdmins = tripleAdminService.getTripleAdminUsers("3");
        
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        Long secAdminId = (Long) secAdmins.get(0).get("id");
        Long auditAdminId = (Long) auditAdmins.get(0).get("id");
        
        // 系统管理员尝试操作安全管理员 - 应该失败
        assertThrows(BusinessException.class, () -> 
            tripleAdminService.validateTripleAdminOperation(sysAdminId, secAdminId));
        
        // 安全管理员尝试操作系统管理员 - 应该失败
        assertThrows(BusinessException.class, () -> 
            tripleAdminService.validateTripleAdminOperation(secAdminId, sysAdminId));
        
        // 审计管理员尝试操作安全管理员 - 应该失败
        assertThrows(BusinessException.class, () -> 
            tripleAdminService.validateTripleAdminOperation(auditAdminId, secAdminId));
    }

    @Test
    @DisplayName("三员不能删除自己的账号")
    void testTripleAdminCannotDeleteSelf() {
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        
        // 不能删除自己
        assertThrows(BusinessException.class, () -> 
            tripleAdminService.validateCannotDeleteSelf(sysAdminId, sysAdminId));
    }

    @Test
    @DisplayName("系统管理员可以管理普通用户")
    void testSystemAdminCanManageRegularUser() {
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        
        // 普通用户ID（非三员）
        Long regularUserId = 999999L;
        
        // 系统管理员可以管理普通用户 - 应该成功（不抛异常）
        assertDoesNotThrow(() -> 
            tripleAdminService.validateTripleAdminOperation(sysAdminId, regularUserId));
    }

    @Test
    @DisplayName("权限决策器-系统管理员权限")
    void testSystemAdminPermission() {
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        
        // 系统管理员可以管理普通用户
        assertTrue(permissionEvaluator.canManageUser(sysAdminId, 999999L));
        
        // 系统管理员不能管理其他三员
        List<Map<String, Object>> secAdmins = tripleAdminService.getTripleAdminUsers("2");
        Long secAdminId = (Long) secAdmins.get(0).get("id");
        assertFalse(permissionEvaluator.canManageUser(sysAdminId, secAdminId));
    }

    @Test
    @DisplayName("权限决策器-审计管理员不能进行业务操作")
    void testAuditAdminCannotDoBusinessOperation() {
        List<Map<String, Object>> auditAdmins = tripleAdminService.getTripleAdminUsers("3");
        Long auditAdminId = (Long) auditAdmins.get(0).get("id");
        
        // 审计管理员不能进行业务写操作
        assertThrows(BusinessException.class, () -> 
            permissionEvaluator.checkPermission(auditAdminId, 999999L, "create"));
    }

    @Test
    @DisplayName("获取三员操作审计日志")
    void testGetTripleAdminAuditLogs() {
        // 查询审计日志（可能为空）
        var logs = tripleAdminService.getTripleAdminAuditLogs(null, null, null, null, 1, 10);
        assertNotNull(logs);
    }

    @Test
    @DisplayName("获取用户三员类型")
    void testGetAdminType() {
        List<Map<String, Object>> sysAdmins = tripleAdminService.getTripleAdminUsers("1");
        Long sysAdminId = (Long) sysAdmins.get(0).get("id");
        
        AdminType type = tripleAdminService.getAdminType(sysAdminId);
        assertEquals(AdminType.SYSTEM_ADMIN, type);
    }
}
