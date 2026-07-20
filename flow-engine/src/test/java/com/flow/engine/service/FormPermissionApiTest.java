package com.flow.engine.service;

import com.flow.engine.dto.*;
import com.flow.engine.entity.Task;
import com.flow.engine.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表单权限API集成测试（ISSUE-009）
 * 测试不同任务返回不同权限
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FormPermissionApiTest {

    @Autowired
    private ProcessDefinitionService definitionService;

    @Autowired
    private ProcessInstanceService instanceService;

    @Autowired
    private FormPermissionService formPermissionService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskService taskService;

    /**
     * 流程定义：两个节点，分别配置不同的表单权限
     */
    private static final String PROCESS_JSON = """
            {
              "processKey": "perm_test",
              "processName": "权限测试流程",
              "nodes": [
                {"id": "start", "type": "start", "name": "开始"},
                {
                  "id": "task1", "type": "userTask", "name": "第一审批",
                  "assignee": "user1",
                  "properties": {
                    "formKey": "leave_form",
                    "formPermissions": {
                      "nodePermission": "edit",
                      "fieldPermissions": [
                        {"fieldKey": "applicant", "permission": "edit"},
                        {"fieldKey": "reason", "permission": "edit"},
                        {"fieldKey": "amount", "permission": "readonly"}
                      ],
                      "buttonPermissions": [
                        {"buttonKey": "submit", "visible": true},
                        {"buttonKey": "reject", "visible": true}
                      ]
                    }
                  }
                },
                {
                  "id": "task2", "type": "userTask", "name": "第二审批",
                  "assignee": "user2",
                  "properties": {
                    "formKey": "leave_form",
                    "inheritFrom": "task1",
                    "formPermissions": {
                      "nodePermission": "readonly",
                      "fieldPermissions": [
                        {"fieldKey": "amount", "permission": "edit", "condition": "amount > 1000", "defaultPermission": "readonly"}
                      ]
                    }
                  }
                },
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
            req.setProcessKey("perm_test");
            req.setProcessName("权限测试流程");
            req.setProcessJson(PROCESS_JSON);
            definitionService.create(req);
            var list = definitionService.list("perm_test", null, null);
            if (!list.isEmpty()) {
                definitionService.deploy(list.get(0).getId());
            }
        } catch (Exception ignored) {
        }
    }

    // ==================== 节点级权限 ====================

    @Test
    @DisplayName("节点级 edit 权限")
    @Order(1)
    void testNodeEditPermission() {
        startProcess();
        Task task = findTask("task1");
        assertNotNull(task, "task1 应存在");

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task.getId());
        assertEquals("edit", resp.getNodePermission());
        assertEquals("leave_form", resp.getFormKey());
    }

    @Test
    @DisplayName("节点级 readonly 权限")
    @Order(2)
    void testNodeReadonlyPermission() {
        startProcess();
        Task task1 = findTask("task1");
        assertNotNull(task1);

        // 完成 task1，进入 task2
        Task task2 = findTask("task2");
        // task2 此时应该不存在（流程停在 task1）
        // 先完成 task1
        completeTask(task1.getId(), "user1");

        task2 = findTask("task2");
        assertNotNull(task2, "task2 应存在");

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task2.getId());
        assertEquals("readonly", resp.getNodePermission());
    }

    // ==================== 字段级权限 ====================

    @Test
    @DisplayName("字段级权限正确返回")
    @Order(3)
    void testFieldPermissions() {
        startProcess();
        Task task = findTask("task1");
        assertNotNull(task);

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task.getId());
        assertNotNull(resp.getFieldPermissions());
        assertEquals(3, resp.getFieldPermissions().size());

        // amount 应为 readonly
        var amountPerm = resp.getFieldPermissions().stream()
                .filter(f -> "amount".equals(f.getFieldKey()))
                .findFirst().orElse(null);
        assertNotNull(amountPerm);
        assertEquals("readonly", amountPerm.getPermission());
    }

    // ==================== 按钮级权限 ====================

    @Test
    @DisplayName("按钮级权限正确返回")
    @Order(4)
    void testButtonPermissions() {
        startProcess();
        Task task = findTask("task1");
        assertNotNull(task);

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task.getId());
        assertNotNull(resp.getButtonPermissions());

        // submit 和 reject 应可见
        var submitBtn = resp.getButtonPermissions().stream()
                .filter(b -> "submit".equals(b.getButtonKey()))
                .findFirst().orElse(null);
        assertNotNull(submitBtn);
        assertTrue(submitBtn.getVisible());
    }

    // ==================== 继承覆盖 ====================

    @Test
    @DisplayName("继承覆盖：子节点继承父节点字段权限")
    @Order(5)
    void testInheritance() {
        startProcess();
        Task task1 = findTask("task1");
        completeTask(task1.getId(), "user1");

        Task task2 = findTask("task2");
        assertNotNull(task2, "task2 应存在");

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task2.getId());
        // task2 继承了 task1 的字段权限，但覆盖了 amount
        assertNotNull(resp.getFieldPermissions());
        // 继承的 applicant(readonly) + reason(edit) + 覆盖的 amount
        assertTrue(resp.getFieldPermissions().size() >= 3,
                "应包含继承的字段和覆盖的字段");

        // amount 字段在 task2 中有条件化配置
        var amountPerm = resp.getFieldPermissions().stream()
                .filter(f -> "amount".equals(f.getFieldKey()))
                .findFirst().orElse(null);
        assertNotNull(amountPerm);
    }

    // ==================== 条件化权限 ====================

    @Test
    @DisplayName("条件化权限：amount > 1000 时 amount 为 edit")
    @Order(6)
    void testConditionalPermissionHighAmount() {
        // 发起流程并设置变量 amount = 2000
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("perm_test");
        request.setStartUser("starter");
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 2000);
        request.setVariables(vars);
        instanceService.start(request);

        Task task1 = findTask("task1");
        completeTask(task1.getId(), "user1");

        Task task2 = findTask("task2");
        assertNotNull(task2);

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task2.getId());
        var amountPerm = resp.getFieldPermissions().stream()
                .filter(f -> "amount".equals(f.getFieldKey()))
                .findFirst().orElse(null);
        assertNotNull(amountPerm);
        assertEquals("edit", amountPerm.getPermission(),
                "amount > 1000 时应为 edit");
    }

    @Test
    @DisplayName("条件化权限：amount <= 1000 时 amount 为 readonly")
    @Order(7)
    void testConditionalPermissionLowAmount() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("perm_test");
        request.setStartUser("starter");
        Map<String, Object> vars = new HashMap<>();
        vars.put("amount", 500);
        request.setVariables(vars);
        instanceService.start(request);

        Task task1 = findTask("task1");
        completeTask(task1.getId(), "user1");

        Task task2 = findTask("task2");
        assertNotNull(task2);

        FormPermissionResponse resp = formPermissionService.getFormPermissions(task2.getId());
        var amountPerm = resp.getFieldPermissions().stream()
                .filter(f -> "amount".equals(f.getFieldKey()))
                .findFirst().orElse(null);
        assertNotNull(amountPerm);
        assertEquals("readonly", amountPerm.getPermission(),
                "amount <= 1000 时应为 readonly");
    }

    // ==================== 辅助方法 ====================

    private void startProcess() {
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey("perm_test");
        request.setStartUser("starter");
        instanceService.start(request);
    }

    private Task findTask(String nodeId) {
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getNodeId, nodeId)
                        .eq(Task::getTaskType, 1)
                        .orderByDesc(Task::getCreateTime)
                        .last("LIMIT 1"));
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    private void completeTask(Long taskId, String userId) {
        taskService.complete(taskId, userId, null);
    }
}
