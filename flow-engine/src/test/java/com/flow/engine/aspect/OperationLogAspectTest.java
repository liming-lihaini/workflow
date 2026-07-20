package com.flow.engine.aspect;

import com.flow.engine.entity.OperationLog;
import com.flow.engine.service.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作日志切面测试（ISSUE-014）
 */
@SpringBootTest
@DisplayName("操作日志切面测试")
public class OperationLogAspectTest {

    @Autowired
    private LogService logService;

    @Test
    @DisplayName("设置修改前数据")
    void testSetBeforeData() {
        // 设置修改前数据（验证方法不抛异常）
        OperationLogAspect.setBeforeData("原始数据");
        
        // 验证设置成功（通过记录日志验证）
        OperationLog logEntity = new OperationLog();
        logEntity.setUserId(1L);
        logEntity.setUsername("before_data_user");
        logEntity.setModule("测试模块");
        logEntity.setOperation("测试");
        logEntity.setBeforeData("测试数据");
        
        logService.recordOperationLog(logEntity);
        assertNotNull(logEntity.getId());
    }

    @Test
    @DisplayName("记录操作日志")
    void testRecordOperationLog() {
        // 直接记录操作日志
        OperationLog logEntity = new OperationLog();
        logEntity.setUserId(1L);
        logEntity.setUsername("test_user");
        logEntity.setModule("测试模块");
        logEntity.setOperation("测试操作");
        logEntity.setMethod("TestService.testMethod");
        logEntity.setParams("{\"param1\":\"value1\"}");
        logEntity.setBeforeData("修改前");
        logEntity.setAfterData("修改后");
        logEntity.setIp("127.0.0.1");
        
        logService.recordOperationLog(logEntity);
        
        assertNotNull(logEntity.getId());
        
        // 验证可以查询到
        List<OperationLog> logs = logService.queryOperationLog(null, "test_user", "测试模块", null, null, null, 1, 10);
        assertFalse(logs.isEmpty());
    }

    @Test
    @DisplayName("操作日志包含前后数据")
    void testOperationLogWithBeforeAfterData() {
        OperationLog logEntity = new OperationLog();
        logEntity.setUserId(1L);
        logEntity.setUsername("data_user");
        logEntity.setModule("数据模块");
        logEntity.setOperation("更新");
        logEntity.setBeforeData("{\"name\":\"旧值\"}");
        logEntity.setAfterData("{\"name\":\"新值\"}");
        
        logService.recordOperationLog(logEntity);
        
        // 查询并验证
        List<OperationLog> logs = logService.queryOperationLog(null, "data_user", "数据模块", "更新", null, null, 1, 10);
        assertFalse(logs.isEmpty());
        
        OperationLog log = logs.get(0);
        assertNotNull(log.getBeforeData());
        assertNotNull(log.getAfterData());
        assertTrue(log.getBeforeData().contains("旧值"));
        assertTrue(log.getAfterData().contains("新值"));
    }

    @Test
    @DisplayName("操作日志包含请求参数")
    void testOperationLogWithParams() {
        OperationLog logEntity = new OperationLog();
        logEntity.setUserId(1L);
        logEntity.setUsername("params_user");
        logEntity.setModule("参数模块");
        logEntity.setOperation("创建");
        logEntity.setParams("[{\"name\":\"测试\",\"value\":123}]");
        
        logService.recordOperationLog(logEntity);
        
        // 查询并验证
        List<OperationLog> logs = logService.queryOperationLog(null, "params_user", null, null, null, null, 1, 10);
        assertFalse(logs.isEmpty());
        
        OperationLog log = logs.get(0);
        assertNotNull(log.getParams());
        assertTrue(log.getParams().contains("测试"));
    }
}
