package com.flow.engine.engine;

import com.flow.engine.common.enums.TaskStatus;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.dto.TaskResponse;
import com.flow.engine.entity.Task;
import com.flow.engine.mapper.TaskMapper;
import com.flow.engine.service.ProcessDefinitionService;
import com.flow.engine.service.ProcessInstanceService;
import com.flow.engine.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户任务自动创建测试（ISSUE-005）
 * 验证流程流转到 userTask 时自动创建任务
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserTaskCreationTest {

    @Autowired
    private ProcessDefinitionService definitionService;

    @Autowired
    private ProcessInstanceService instanceService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    private static final String PROCESS_JSON = """
            {
              "processKey": "auto_task_test",
              "processName": "自动建任务测试",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "task1", "type": "userTask", "name": "一级审批", "assignee": "autoUser1"},
                {"id": "task2", "type": "userTask", "name": "二级审批", "candidateUsers": "autoUser2,autoUser3"},
                {"id": "end", "type": "end", "name": "结束"}
              ],
              "edges": [
                {"id": "e1", "source": "start", "target": "task1"},
                {"id": "e2", "source": "task1", "target": "task2"},
                {"id": "e3", "source": "task2", "target": "end"}
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        try {
            ProcessDefinitionCreateRequest req = new ProcessDefinitionCreateRequest();
            req.setProcessKey("auto_task_test");
            req.setProcessName("自动建任务测试");
            req.setProcessJson(PROCESS_JSON);
            definitionService.create(req);
            var list = definitionService.list("auto_task_test", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("发起流程后 userTask 自动创建任务（指定处理人）")
    @Order(1)
    void testAutoCreateTaskWithAssignee() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("auto_task_test");
        request.setStartUser("starter");
        var response = instanceService.start(request);

        // 验证 task1 自动创建，assignee = autoUser1
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "task1")
        );
        assertFalse(tasks.isEmpty(), "task1 应自动创建任务");

        Task task = tasks.get(0);
        assertEquals("autoUser1", task.getAssignee());
        assertEquals("一级审批", task.getNodeName());
        assertEquals(TaskStatus.PENDING.getValue(), task.getStatus());
    }

    @Test
    @DisplayName("完成任务后流转到下一个 userTask，自动创建新任务")
    @Order(2)
    void testAutoCreateSecondTask() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("auto_task_test");
        request.setStartUser("starter");
        var response = instanceService.start(request);

        // 完成 task1
        List<Task> tasks1 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "task1")
        );
        assertFalse(tasks1.isEmpty());
        taskService.complete(tasks1.get(0).getId(), "autoUser1", null);

        // 验证 task2 自动创建（候选人模式）
        List<Task> tasks2 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "task2")
        );
        assertFalse(tasks2.isEmpty(), "task2 应自动创建任务");

        Task task2 = tasks2.get(0);
        assertEquals("二级审批", task2.getNodeName());
        assertEquals("autoUser2,autoUser3", task2.getCandidateUsers());
    }

    @Test
    @DisplayName("完成所有任务后流程结束")
    @Order(3)
    void testCompleteAllTasks() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("auto_task_test");
        request.setStartUser("starter");
        var response = instanceService.start(request);

        // 完成 task1
        List<Task> tasks1 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "task1")
        );
        taskService.complete(tasks1.get(0).getId(), "autoUser1", null);

        // 签收并完成任务2
        List<Task> task2List = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "task2")
        );
        assertFalse(task2List.isEmpty(), "task2 应存在");
        Task task2 = task2List.get(0);
        taskService.claim(task2.getId(), "autoUser2");
        taskService.complete(task2.getId(), "autoUser2", null);

        // 验证流程完成
        var instance = instanceService.getById(response.getId());
        assertEquals(1, instance.getStatus(), "流程应已完成");
    }
}
