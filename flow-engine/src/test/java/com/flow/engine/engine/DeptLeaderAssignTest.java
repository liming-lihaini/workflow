package com.flow.engine.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.common.enums.TaskStatus;
import com.flow.engine.dto.ProcessDefinitionCreateRequest;
import com.flow.engine.dto.StartProcessRequest;
import com.flow.engine.entity.Dept;
import com.flow.engine.entity.Task;
import com.flow.engine.entity.User;
import com.flow.engine.mapper.TaskMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.service.DeptService;
import com.flow.engine.service.ProcessDefinitionService;
import com.flow.engine.service.ProcessInstanceService;
import com.flow.engine.service.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 部门领导分配自动化测试
 * <p>
 * 场景：songjiang（宋江，总经办成员）发起请假流程，
 * 验证节点处理人分配方式为"部门领导"（assigneeType=dept, assignee=deptLeader）时，
 * 任务自动解析为发起人所在部门（总经办）的领导进行审批。
 * <p>
 * 前端设计器输出格式（ConfigPanel.jsx）：
 * - assigneeType = "dept"
 * - assignee = "deptLeader"（直属部门领导）或 "parentDeptLeader"（二级部门领导）
 * - candidateUsers = 同 assignee
 * <p>
 * 测试数据由 TestDataInitializer 初始化：
 * - songjiang（宋江）：用户ID=100，部门=100（总经办）
 * - 部门100的部门领导由 initDeptLeaders() 随机设置
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeptLeaderAssignTest {

    @Autowired
    private ProcessDefinitionService definitionService;

    @Autowired
    private ProcessInstanceService instanceService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private DeptService deptService;

    @Autowired
    private UserMapper userMapper;

    private static Long songjiangDeptId;
    private static String deptLeaderUsername;
    private static boolean initialized = false;
    private static final String PROCESS_KEY = "dept_leader_leave";
    /** 发起账号：songjiang（宋江），总经办成员 */
    private static final String START_USER = "songjiang";

    @BeforeEach
    void setUp() {
        if (initialized) return;

        // 1. 查询 songjiang 用户信息（由 TestDataInitializer 创建）
        User songjiang = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, START_USER));
        assertNotNull(songjiang, "songjiang 用户应由 TestDataInitializer 初始化");
        songjiangDeptId = songjiang.getDeptId();
        assertNotNull(songjiangDeptId, "songjiang 应有部门ID");

        // 2. 查询总经办部门领导
        Dept dept = deptService.getDept(songjiangDeptId);
        if (dept.getLeaderId() != null) {
            User leader = userMapper.selectById(dept.getLeaderId());
            assertNotNull(leader, "部门领导用户应存在");
            deptLeaderUsername = leader.getUsername();
        } else {
            deptService.setLeader(songjiangDeptId, songjiang.getId(), songjiang.getRealName());
            deptLeaderUsername = songjiang.getUsername();
        }

        System.out.println("=== 测试数据 ===");
        System.out.println("发起人: " + songjiang.getUsername() + "（" + songjiang.getRealName() + "）");
        System.out.println("所属部门: " + dept.getDeptName() + "（ID=" + dept.getId() + "）");
        System.out.println("部门领导: " + deptLeaderUsername);

        // 3. 创建请假流程定义 —— 使用前端设计器的实际输出格式
        //    assigneeType = "dept", assignee = "deptLeader"（匹配 ConfigPanel.jsx）
        String processJson = """
                {
                  "processKey": "dept_leader_leave",
                  "processName": "请假审批流程",
                  "nodes": [
                    {"id": "start", "type": "start", "name": "开始"},
                    {"id": "dept_approve", "type": "userTask", "name": "部门领导审批",
                     "assigneeType": "dept", "assignee": "deptLeader", "candidateUsers": "deptLeader"},
                    {"id": "hr_approve", "type": "userTask", "name": "HR审批",
                     "assignee": "hrUser"},
                    {"id": "end", "type": "end", "name": "结束"}
                  ],
                  "edges": [
                    {"id": "e1", "source": "start", "target": "dept_approve"},
                    {"id": "e2", "source": "dept_approve", "target": "hr_approve"},
                    {"id": "e3", "source": "hr_approve", "target": "end"}
                  ]
                }
                """;

        ProcessDefinitionCreateRequest req = new ProcessDefinitionCreateRequest();
        req.setProcessKey(PROCESS_KEY);
        req.setProcessName("请假审批流程");
        req.setProcessJson(processJson);
        definitionService.create(req);
        var list = definitionService.list(PROCESS_KEY, null, null);
        if (!list.isEmpty()) {
            definitionService.deploy(list.get(0).getId());
        }
        System.out.println("流程定义已创建并部署: " + PROCESS_KEY);
        System.out.println("===============");
        initialized = true;
    }

    @Test
    @DisplayName("songjiang 发起请假，部门领导审批任务自动分配给实际部门领导（非字面量 deptLeader）")
    @Order(1)
    void testDeptLeaderAssigned() {
        assertTrue(initialized, "初始化应完成");

        // songjiang 发起请假流程
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey(PROCESS_KEY);
        request.setStartUser(START_USER);
        var response = instanceService.start(request);
        assertNotNull(response.getId(), "流程实例应创建成功");

        // 验证部门领导审批节点已创建
        List<Task> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "dept_approve")
        );
        assertFalse(tasks.isEmpty(), "部门领导审批节点应自动创建任务");

        Task task = tasks.get(0);

        // 核心验证：assignee 必须是实际的 username，不能是字面量 "deptLeader"
        assertNotEquals("deptLeader", task.getAssignee(),
                "assignee 不能是字面量 'deptLeader'，必须解析为实际用户");
        assertEquals(deptLeaderUsername, task.getAssignee(),
                "任务处理人应为部门领导 " + deptLeaderUsername + ", actual=" + task.getAssignee());

        // candidateUsers 也应为空（部门模式下不需要候选人）
        assertNull(task.getCandidateUsers(),
                "部门领导模式下 candidateUsers 应为 null, actual=" + task.getCandidateUsers());

        assertEquals("部门领导审批", task.getNodeName());
        assertEquals(TaskStatus.PENDING.getValue(), task.getStatus(), "任务状态应为待处理");

        // 验证流程发起人
        var instance = instanceService.getById(response.getId());
        assertEquals(START_USER, instance.getStartUser(), "流程发起人应为 songjiang");
    }

    @Test
    @DisplayName("部门领导签收并完成审批后，流转到 HR 审批节点")
    @Order(2)
    void testDeptLeaderCompleteTask() {
        // songjiang 发起请假流程
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey(PROCESS_KEY);
        request.setStartUser(START_USER);
        var response = instanceService.start(request);

        // 查找部门领导审批任务
        List<Task> tasks1 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "dept_approve")
        );
        assertFalse(tasks1.isEmpty(), "部门领导审批任务应存在");
        Task task1 = tasks1.get(0);
        assertEquals(deptLeaderUsername, task1.getAssignee(), "处理人应为部门领导");

        // 部门领导签收
        taskService.claim(task1.getId(), deptLeaderUsername);
        Task claimedTask = taskMapper.selectById(task1.getId());
        assertEquals(TaskStatus.IN_PROGRESS.getValue(), claimedTask.getStatus(), "签收后状态应为处理中");

        // 部门领导审批通过
        taskService.complete(task1.getId(), deptLeaderUsername, null);
        Task completedTask = taskMapper.selectById(task1.getId());
        assertEquals(TaskStatus.COMPLETED.getValue(), completedTask.getStatus(), "完成后状态应为已完成");

        // 验证流转到 HR 审批节点
        List<Task> tasks2 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "hr_approve")
        );
        assertFalse(tasks2.isEmpty(), "HR审批节点应自动创建任务");
        assertEquals("hrUser", tasks2.get(0).getAssignee(), "HR审批处理人应为 hrUser");
    }

    @Test
    @DisplayName("songjiang 发起 -> 部门领导审批 -> HR审批 -> 流程结束（完整链路）")
    @Order(3)
    void testFullProcessComplete() {
        // songjiang 发起请假流程
        StartProcessRequest request = new StartProcessRequest();
        request.setProcessKey(PROCESS_KEY);
        request.setStartUser(START_USER);
        var response = instanceService.start(request);

        // 部门领导完成审批
        List<Task> tasks1 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "dept_approve")
        );
        assertFalse(tasks1.isEmpty(), "部门领导审批任务应存在");
        taskService.claim(tasks1.get(0).getId(), deptLeaderUsername);
        taskService.complete(tasks1.get(0).getId(), deptLeaderUsername, null);

        // HR 完成审批
        List<Task> tasks2 = taskMapper.selectList(
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getProcessInstanceId, response.getId())
                        .eq(Task::getNodeId, "hr_approve")
        );
        assertFalse(tasks2.isEmpty(), "HR审批任务应存在");
        taskService.claim(tasks2.get(0).getId(), "hrUser");
        taskService.complete(tasks2.get(0).getId(), "hrUser", null);

        // 验证流程结束
        var instance = instanceService.getById(response.getId());
        assertNotNull(instance, "流程实例应存在");
        assertEquals(1, instance.getStatus(), "所有任务完成后流程应结束");
    }
}
