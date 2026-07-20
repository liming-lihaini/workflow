package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.entity.ProcessDefinition;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.entity.Task;
import com.flow.engine.entity.Variable;
import com.flow.engine.mapper.ProcessDefinitionMapper;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.mapper.TaskMapper;
import com.flow.engine.mapper.VariableMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 流程监控服务测试（ISSUE-017）
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("流程监控服务测试")
public class ProcessMonitorServiceTest {

    @Autowired
    private ProcessMonitorService processMonitorService;

    @Autowired
    private ProcessDefinitionMapper processDefinitionMapper;

    @Autowired
    private ProcessInstanceMapper processInstanceMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private VariableMapper variableMapper;

    private Long testInstanceId;

    @BeforeEach
    void setUp() {
        // 创建测试流程定义
        ProcessDefinition definition = new ProcessDefinition();
        definition.setProcessKey("monitor_test_" + System.currentTimeMillis());
        definition.setProcessName("监控测试流程");
        definition.setVersion(1);
        definition.setStatus(1);
        definition.setCreateTime(LocalDateTime.now());
        definition.setUpdateTime(LocalDateTime.now());
        processDefinitionMapper.insert(definition);

        // 创建测试流程实例
        ProcessInstance instance = new ProcessInstance();
        instance.setProcessKey(definition.getProcessKey());
        instance.setProcessName(definition.getProcessName());
        instance.setProcessVersion(1);
        instance.setBusinessKey("BIZ_" + System.currentTimeMillis());
        instance.setStatus(0); // 运行中
        instance.setCurrentNodeId("task1");
        instance.setStartUser("testUser");
        instance.setStartTime(LocalDateTime.now().minusHours(1));
        instance.setCreateTime(LocalDateTime.now().minusHours(1));
        instance.setUpdateTime(LocalDateTime.now());
        processInstanceMapper.insert(instance);
        testInstanceId = instance.getId();

        // 创建测试任务
        Task task1 = new Task();
        task1.setProcessInstanceId(testInstanceId);
        task1.setProcessKey(definition.getProcessKey());
        task1.setNodeId("task1");
        task1.setNodeName("任务1");
        task1.setTaskType(1);
        task1.setAssignee("user1");
        task1.setStatus(2); // 已完成
        task1.setCreateTime(LocalDateTime.now().minusMinutes(50));
        task1.setCompleteTime(LocalDateTime.now().minusMinutes(30));
        task1.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task1);

        Task task2 = new Task();
        task2.setProcessInstanceId(testInstanceId);
        task2.setProcessKey(definition.getProcessKey());
        task2.setNodeId("task2");
        task2.setNodeName("任务2");
        task2.setTaskType(1);
        task2.setAssignee("user2");
        task2.setStatus(0); // 待处理
        task2.setCreateTime(LocalDateTime.now().minusMinutes(30));
        task2.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task2);

        // 创建测试变量
        Variable var1 = new Variable();
        var1.setProcessInstanceId(testInstanceId);
        var1.setVariableKey("amount");
        var1.setVariableValue("1000");
        var1.setVariableType("number");
        var1.setCreateTime(LocalDateTime.now().minusMinutes(45));
        variableMapper.insert(var1);

        Variable var2 = new Variable();
        var2.setProcessInstanceId(testInstanceId);
        var2.setTaskId(task1.getId());
        var2.setVariableKey("approved");
        var2.setVariableValue("true");
        var2.setVariableType("boolean");
        var2.setCreateTime(LocalDateTime.now().minusMinutes(35));
        variableMapper.insert(var2);
    }

    @Test
    @DisplayName("获取执行轨迹")
    void testGetExecutionHistory() {
        List<Map<String, Object>> history = processMonitorService.getExecutionHistory(testInstanceId);
        
        assertNotNull(history);
        assertTrue(history.size() >= 3, "至少包含开始节点和2个任务节点");
        
        // 验证开始节点
        Map<String, Object> startNode = history.get(0);
        assertEquals("start", startNode.get("nodeId"));
        assertEquals("开始", startNode.get("nodeName"));
        
        // 验证任务节点
        Map<String, Object> taskNode = history.get(1);
        assertEquals("task1", taskNode.get("nodeId"));
        assertEquals("completed", taskNode.get("status"));
    }

    @Test
    @DisplayName("获取变量历史")
    void testGetVariableHistory() {
        List<Map<String, Object>> variables = processMonitorService.getVariableHistory(testInstanceId);
        
        assertNotNull(variables);
        assertEquals(2, variables.size());
        
        // 验证变量内容
        Map<String, Object> var1 = variables.get(0);
        assertEquals("amount", var1.get("variableKey"));
        assertEquals("1000", var1.get("variableValue"));
    }

    @Test
    @DisplayName("获取耗时统计")
    void testGetStatistics() {
        Map<String, Object> stats = processMonitorService.getStatistics(testInstanceId);
        
        assertNotNull(stats);
        assertNotNull(stats.get("totalDuration"));
        
        // 验证节点统计
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeStats = (List<Map<String, Object>>) stats.get("nodeStatistics");
        assertNotNull(nodeStats);
        assertEquals(2, nodeStats.size());
        
        // 验证任务统计
        assertEquals(2, stats.get("totalTasks"));
        assertEquals(1L, stats.get("completedTasks"));
        assertEquals(1L, stats.get("pendingTasks"));
    }

    @Test
    @DisplayName("获取运行中的流程列表")
    void testGetRunningProcesses() {
        List<Map<String, Object>> running = processMonitorService.getRunningProcesses(
                null, null, null, null, 1, 10);
        
        assertNotNull(running);
        assertTrue(running.size() >= 1);
        
        // 验证包含测试实例
        boolean found = running.stream()
                .anyMatch(r -> testInstanceId.equals(r.get("id")));
        assertTrue(found);
    }

    @Test
    @DisplayName("导出实例数据")
    void testExportInstanceData() {
        Map<String, Object> exportData = processMonitorService.exportInstanceData(testInstanceId);
        
        assertNotNull(exportData);
        assertNotNull(exportData.get("basicInfo"));
        assertNotNull(exportData.get("executionHistory"));
        assertNotNull(exportData.get("variableHistory"));
        assertNotNull(exportData.get("statistics"));
    }

    @Test
    @DisplayName("管理员干预-成功")
    void testInterveneSuccess() {
        assertDoesNotThrow(() -> 
            processMonitorService.intervene(testInstanceId, "task3", 1L, "测试干预"));
        
        // 验证当前节点已更新
        ProcessInstance instance = processInstanceMapper.selectById(testInstanceId);
        assertEquals("task3", instance.getCurrentNodeId());
    }

    @Test
    @DisplayName("管理员干预-实例不存在")
    void testInterveneInstanceNotFound() {
        assertThrows(BusinessException.class, () -> 
            processMonitorService.intervene(999999L, "task1", 1L, "测试"));
    }

    @Test
    @DisplayName("管理员干预-实例不在运行状态")
    void testInterveneInstanceNotRunning() {
        // 将实例状态改为已完成
        ProcessInstance instance = processInstanceMapper.selectById(testInstanceId);
        instance.setStatus(1);
        processInstanceMapper.updateById(instance);
        
        assertThrows(BusinessException.class, () -> 
            processMonitorService.intervene(testInstanceId, "task1", 1L, "测试"));
    }

    @Test
    @DisplayName("查询不存在的实例-执行轨迹")
    void testGetExecutionHistoryNotFound() {
        assertThrows(BusinessException.class, () -> 
            processMonitorService.getExecutionHistory(999999L));
    }

    @Test
    @DisplayName("查询不存在的实例-变量历史")
    void testGetVariableHistoryNotFound() {
        assertThrows(BusinessException.class, () -> 
            processMonitorService.getVariableHistory(999999L));
    }

    @Test
    @DisplayName("查询不存在的实例-耗时统计")
    void testGetStatisticsNotFound() {
        assertThrows(BusinessException.class, () -> 
            processMonitorService.getStatistics(999999L));
    }
}
