package com.flow.engine.service;

import com.flow.engine.entity.Variable;
import com.flow.engine.mapper.VariableMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流程变量测试（ISSUE-004）
 */
@SpringBootTest
@ActiveProfiles("test")
class ProcessVariableTest {

    @Autowired
    private VariableService variableService;

    @Autowired
    private VariableMapper variableMapper;

    @Test
    @DisplayName("保存和获取变量")
    void testSaveAndGetVariables() {
        // 使用一个唯一的实例ID
        Long instanceId = System.currentTimeMillis();

        Map<String, Object> vars = new HashMap<>();
        vars.put("days", 5);
        vars.put("reason", "vacation");
        vars.put("approved", true);

        variableService.saveVariables(instanceId, vars);

        Map<String, Object> retrieved = variableService.getVariables(instanceId);
        assertEquals(5, retrieved.get("days"));
        assertEquals("vacation", retrieved.get("reason"));
        assertEquals(true, retrieved.get("approved"));
    }

    @Test
    @DisplayName("更新变量")
    void testUpdateVariables() {
        Long instanceId = System.currentTimeMillis() + 1;

        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 100);
        variableService.saveVariables(instanceId, vars);

        // 更新
        Map<String, Object> update = new HashMap<>();
        update.put("amount", 200);
        update.put("status", "approved");
        variableService.updateVariables(instanceId, update);

        Map<String, Object> retrieved = variableService.getVariables(instanceId);
        assertEquals(200, retrieved.get("amount"));
        assertEquals("approved", retrieved.get("status"));
    }

    @Test
    @DisplayName("变量名以$开头的不保存")
    void testDollarPrefixIgnored() {
        Long instanceId = System.currentTimeMillis() + 2;

        Map<String, Object> vars = new HashMap<>();
        vars.put("$internal", "should_be_ignored");
        vars.put("normal", "value");

        variableService.saveVariables(instanceId, vars);

        Map<String, Object> retrieved = variableService.getVariables(instanceId);
        assertNull(retrieved.get("$internal"));
        assertEquals("value", retrieved.get("normal"));
    }

    @Test
    @DisplayName("空变量不报错")
    void testEmptyVariables() {
        Long instanceId = System.currentTimeMillis() + 3;
        variableService.saveVariables(instanceId, null);
        variableService.saveVariables(instanceId, new HashMap<>());

        Map<String, Object> retrieved = variableService.getVariables(instanceId);
        assertTrue(retrieved.isEmpty());
    }
}
