package com.flow.engine.service;

import com.flow.engine.entity.AccessLog;
import com.flow.engine.entity.OperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 日志服务测试（ISSUE-014）
 */
@SpringBootTest
@DisplayName("日志服务测试")
public class LogServiceTest {

    @Autowired
    private LogService logService;

    @Test
    @DisplayName("记录访问日志")
    void testRecordAccessLog() {
        AccessLog accessLog = new AccessLog();
        accessLog.setUserId(1L);
        accessLog.setUsername("testuser");
        accessLog.setUrl("/api/v1/system/depts");
        accessLog.setMethod("GET");
        accessLog.setIp("127.0.0.1");
        accessLog.setResult(1);
        
        logService.recordAccessLog(accessLog);
        
        assertNotNull(accessLog.getId());
    }

    @Test
    @DisplayName("查询访问日志")
    void testQueryAccessLog() {
        // 先记录几条日志
        AccessLog log1 = new AccessLog();
        log1.setUserId(1L);
        log1.setUsername("query_user_" + System.currentTimeMillis());
        log1.setUrl("/api/v1/test1");
        log1.setMethod("GET");
        log1.setResult(1);
        logService.recordAccessLog(log1);
        
        AccessLog log2 = new AccessLog();
        log2.setUserId(1L);
        log2.setUsername("query_user_" + System.currentTimeMillis());
        log2.setUrl("/api/v1/test2");
        log2.setMethod("POST");
        log2.setResult(1);
        logService.recordAccessLog(log2);
        
        // 查询
        List<AccessLog> logs = logService.queryAccessLog(1L, null, "/api/v1/test", null, null, null, 1, 10);
        assertFalse(logs.isEmpty());
    }

    @Test
    @DisplayName("记录操作日志")
    void testRecordOperationLog() {
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(1L);
        operationLog.setUsername("testuser");
        operationLog.setModule("部门管理");
        operationLog.setOperation("创建");
        operationLog.setMethod("DeptController.createDept");
        operationLog.setParams("{\"deptName\":\"测试部门\"}");
        operationLog.setBeforeData(null);
        operationLog.setAfterData("{\"id\":1,\"deptName\":\"测试部门\"}");
        operationLog.setIp("127.0.0.1");
        
        logService.recordOperationLog(operationLog);
        
        assertNotNull(operationLog.getId());
    }

    @Test
    @DisplayName("查询操作日志")
    void testQueryOperationLog() {
        // 先记录日志
        OperationLog log1 = new OperationLog();
        log1.setUserId(1L);
        log1.setUsername("op_user_" + System.currentTimeMillis());
        log1.setModule("用户管理");
        log1.setOperation("创建");
        logService.recordOperationLog(log1);
        
        // 查询
        List<OperationLog> logs = logService.queryOperationLog(1L, null, "用户管理", null, null, null, 1, 10);
        assertFalse(logs.isEmpty());
    }

    @Test
    @DisplayName("导出访问日志")
    void testExportAccessLog() {
        // 先记录日志
        AccessLog log1 = new AccessLog();
        log1.setUserId(1L);
        log1.setUsername("export_user_" + System.currentTimeMillis());
        log1.setUrl("/api/v1/export/test");
        log1.setMethod("GET");
        log1.setResult(1);
        logService.recordAccessLog(log1);
        
        // 导出
        List<AccessLog> logs = logService.exportAccessLog(null, "export_user", null, null, null, null);
        assertFalse(logs.isEmpty());
    }

    @Test
    @DisplayName("导出操作日志")
    void testExportOperationLog() {
        // 先记录日志
        OperationLog log1 = new OperationLog();
        log1.setUserId(1L);
        log1.setUsername("export_op_user_" + System.currentTimeMillis());
        log1.setModule("导出测试");
        log1.setOperation("测试");
        logService.recordOperationLog(log1);
        
        // 导出
        List<OperationLog> logs = logService.exportOperationLog(null, "export_op_user", null, null, null, null);
        assertFalse(logs.isEmpty());
    }

    @Test
    @DisplayName("清理过期日志")
    void testCleanExpiredLogs() {
        // 记录一条旧日志（模拟）
        AccessLog oldLog = new AccessLog();
        oldLog.setUserId(1L);
        oldLog.setUsername("old_user");
        oldLog.setUrl("/api/v1/old");
        oldLog.setMethod("GET");
        oldLog.setResult(1);
        oldLog.setAccessTime(LocalDateTime.now().minusDays(60)); // 60天前
        logService.recordAccessLog(oldLog);
        
        // 清理30天前的日志
        logService.cleanExpiredLogs(30);
        
        // 验证旧日志被清理
        List<AccessLog> logs = logService.queryAccessLog(null, "old_user", null, null, null, null, 1, 10);
        assertTrue(logs.isEmpty());
    }

    @Test
    @DisplayName("统计访问日志数量")
    void testCountAccessLog() {
        // 先记录日志
        AccessLog log1 = new AccessLog();
        log1.setUserId(1L);
        log1.setUsername("count_user_" + System.currentTimeMillis());
        log1.setUrl("/api/v1/count/test");
        log1.setMethod("GET");
        log1.setResult(1);
        logService.recordAccessLog(log1);
        
        // 统计
        long count = logService.countAccessLog(null, "count_user", null, null, null, null);
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("统计操作日志数量")
    void testCountOperationLog() {
        // 先记录日志
        OperationLog log1 = new OperationLog();
        log1.setUserId(1L);
        log1.setUsername("count_op_user_" + System.currentTimeMillis());
        log1.setModule("统计测试");
        log1.setOperation("测试");
        logService.recordOperationLog(log1);
        
        // 统计
        long count = logService.countOperationLog(null, "count_op_user", null, null, null, null);
        assertTrue(count > 0);
    }
}
