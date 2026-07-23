package com.flow.engine.service;

import com.flow.engine.common.BusinessException;
import com.flow.engine.common.enums.TaskAction;
import com.flow.engine.common.enums.TaskStatus;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.dto.TaskResponse;
import com.flow.engine.entity.Task;
import com.flow.engine.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务服务测试（ISSUE-005）
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ProcessDefinitionService definitionService;

    @Autowired
    private ProcessInstanceService instanceService;

    private static final String PROCESS_JSON = """
            {
              "processKey": "task_test",
              "processName": "任务测试流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {"id": "task1", "type": "userTask", "name": "审批", "assignee": "user1", "candidateUsers": "user1,user2"},
                {"id": "task2", "type": "userTask", "name": "二级审批"},
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
            req.setProcessKey("task_test");
            req.setProcessName("任务测试流程");
            req.setProcessJson(PROCESS_JSON);
            definitionService.create(req);
            var list = definitionService.list("task_test", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("创建任务：自动建任务并查询")
    @Order(1)
    void testCreateAndQuery() {
        // 发起流程 -> userTask 自动创建任务
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        // 查询待办
        List<TaskResponse> todos = taskService.getTodoList("user1");
        assertFalse(todos.isEmpty(), "user1 应有待办任务");

        TaskResponse task = todos.get(0);
        assertEquals("task1", task.getNodeId());
        assertEquals("审批", task.getNodeName());
        assertEquals("user1", task.getAssignee());
        assertEquals(TaskStatus.PENDING.getValue(), task.getStatus());
    }

    @Test
    @DisplayName("签收：候选人签收后状态变为处理中")
    @Order(2)
    void testClaim() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        assertFalse(todos.isEmpty());
        Long taskId = todos.get(0).getId();

        // 签收
        TaskResponse claimed = taskService.claim(taskId, "user1");
        assertEquals(TaskStatus.IN_PROGRESS.getValue(), claimed.getStatus());
        assertEquals("user1", claimed.getAssignee());
        assertNotNull(claimed.getClaimTime());
    }

    @Test
    @DisplayName("取消签收：回到待处理状态")
    @Order(3)
    void testUnclaim() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        taskService.claim(taskId, "user1");
        TaskResponse unclaimed = taskService.unclaim(taskId);
        assertEquals(TaskStatus.PENDING.getValue(), unclaimed.getStatus());
    }

    @Test
    @DisplayName("通过：任务完成并触发流程推进")
    @Order(4)
    void testComplete() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        TaskResponse completed = taskService.complete(taskId, "user1", null);
        assertEquals(TaskStatus.COMPLETED.getValue(), completed.getStatus());
        assertEquals(TaskAction.APPROVED.getValue(), completed.getTaskAction());
        assertNotNull(completed.getCompleteTime());

        // 已办列表应有该任务
        List<TaskResponse> dones = taskService.getDoneList("user1");
        assertTrue(dones.stream().anyMatch(t -> t.getId().equals(taskId)));
    }

    @Test
    @DisplayName("转办：原任务标记转办，新任务给目标用户")
    @Order(5)
    void testTransfer() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        TaskResponse newTask = taskService.transfer(taskId, "user1", "user3");
        assertEquals("user3", newTask.getAssignee());
        assertEquals(TaskStatus.PENDING.getValue(), newTask.getStatus());
        assertEquals(TaskAction.NORMAL.getValue(), newTask.getTaskAction());

        // 原任务应为转办完成
        TaskResponse original = taskService.getById(taskId);
        assertEquals(TaskStatus.COMPLETED.getValue(), original.getStatus());
        assertEquals(TaskAction.TRANSFERRED.getValue(), original.getTaskAction());
    }

    @Test
    @DisplayName("委派：原任务标记委派，新任务给受托人")
    @Order(6)
    void testDelegate() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        TaskResponse newTask = taskService.delegate(taskId, "user1", "user4");
        assertEquals("user4", newTask.getAssignee());
        assertEquals(TaskStatus.PENDING.getValue(), newTask.getStatus());

        // 原任务应为委派完成
        TaskResponse original = taskService.getById(taskId);
        assertEquals(TaskStatus.COMPLETED.getValue(), original.getStatus());
        assertEquals(TaskAction.DELEGATED.getValue(), original.getTaskAction());
    }

    @Test
    @DisplayName("权限校验：非签收人操作被拒绝")
    @Order(7)
    void testPermissionCheck() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        // user1 签收
        taskService.claim(taskId, "user1");

        // user2 尝试完成 -> 应被拒绝
        assertThrows(BusinessException.class, () -> {
            taskService.complete(taskId, "user2", null);
        });
    }

    @Test
    @DisplayName("驳回：任务标记驳回，流程回退")
    @Order(8)
    void testReject() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("task_test");
        request.setStartUser("starter");
        instanceService.start(request);

        List<TaskResponse> todos = taskService.getTodoList("user1");
        Long taskId = todos.get(0).getId();

        TaskResponse rejected = taskService.reject(taskId, "user1", null, null);
        assertEquals(TaskStatus.COMPLETED.getValue(), rejected.getStatus());
        assertEquals(TaskAction.REJECTED.getValue(), rejected.getTaskAction());
    }
}
