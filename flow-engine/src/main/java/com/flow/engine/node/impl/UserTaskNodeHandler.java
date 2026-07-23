package com.flow.engine.node.impl;

import com.flow.engine.entity.Dept;
import com.flow.engine.entity.ProcessInstance;
import com.flow.engine.entity.User;
import com.flow.engine.event.NodeEnteredEvent;
import com.flow.engine.mapper.ProcessInstanceMapper;
import com.flow.engine.mapper.UserMapper;
import com.flow.engine.node.ExecutionContext;
import com.flow.engine.node.NodeHandler;
import com.flow.engine.service.DeptService;
import com.flow.engine.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 用户任务节点处理器（ISSUE-005）
 * <p>
 * 监听 NodeEnteredEvent，当 nodeType="userTask" 时自动创建任务。
 * 支持处理人分配方式：
 * - user: 指定用户（assignee/candidateUsers 直接存储用户名）
 * - dept: 部门领导（assignee=deptLeader/parentDeptLeader，运行时动态解析）
 * - role: 角色
 * - expression: 表单表达式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTaskNodeHandler implements NodeHandler {

    private final TaskService taskService;
    private final DeptService deptService;
    private final UserMapper userMapper;
    private final ProcessInstanceMapper processInstanceMapper;

    @Override
    public String getNodeType() {
        return "userTask";
    }

    @Override
    public void onEnter(ExecutionContext context) {
        // 任务创建由 @EventListener 处理，避免重复
    }

    @Override
    public void execute(ExecutionContext context) {
        // userTask 等待外部完成，此处无逻辑
    }

    @Override
    public void onLeave(ExecutionContext context) {
        // 清理逻辑（暂无）
    }

    /**
     * 监听节点进入事件，为 userTask 自动创建任务
     */
    @EventListener
    public void onNodeEntered(NodeEnteredEvent event) {
        if (!"userTask".equals(event.getNodeType())) {
            return;
        }
        if (event.getProcessInstanceId() == null) {
            log.warn("[UserTaskNodeHandler] processInstanceId 为空，跳过任务创建");
            return;
        }

        String assignee = event.getAssignee();
        String candidateUsers = event.getCandidateUsers();
        String assigneeType = event.getAssigneeType();

        // === 部门领导分配：assigneeType=dept（前端设计器）或 dept_leader（API 直传）===
        boolean isDeptType = "dept".equals(assigneeType) || "dept_leader".equals(assigneeType);
        boolean isDeptLeader = "deptLeader".equals(assignee);
        boolean isParentDeptLeader = "parentDeptLeader".equals(assignee);

        if (isDeptType && (isDeptLeader || isParentDeptLeader)) {
            Long deptId = resolveApplicantDeptId(event, isParentDeptLeader);
            if (deptId != null) {
                String resolvedAssignee = resolveDeptLeaderUsername(deptId);
                // 二级部门领导未找到时，回退到直属部门领导
                if (resolvedAssignee == null && isParentDeptLeader) {
                    Long fallbackDeptId = resolveApplicantDeptId(event, false);
                    if (fallbackDeptId != null) {
                        resolvedAssignee = resolveDeptLeaderUsername(fallbackDeptId);
                        log.info("[UserTaskNodeHandler] 二级部门领导不存在，回退到直属部门领导: deptId={}, leader={}",
                                fallbackDeptId, resolvedAssignee);
                    }
                }
                if (resolvedAssignee != null) {
                    assignee = resolvedAssignee;
                    candidateUsers = null; // 部门领导模式下不需要候选人
                    log.info("[UserTaskNodeHandler] 部门领导解析成功: deptId={}, leader={}, parentDept={}",
                            deptId, assignee, isParentDeptLeader);
                } else {
                    log.warn("[UserTaskNodeHandler] 部门 {} 未找到领导", deptId);
                }
            } else {
                log.warn("[UserTaskNodeHandler] 无法确定部门ID, instanceId={}, assignee={}",
                        event.getProcessInstanceId(), assignee);
            }
        }

        log.info("[UserTaskNodeHandler] userTask 节点进入: instanceId={}, nodeId={}, assignee={}, candidates={}, assigneeType={}",
                event.getProcessInstanceId(), event.getNodeId(),
                assignee, candidateUsers, assigneeType);

        taskService.createTask(
                event.getProcessInstanceId(),
                event.getProcessKey(),
                event.getNodeId(),
                event.getNodeName(),
                assignee,
                candidateUsers
        );
    }

    /**
     * 解析申请人（发起人）的直属部门ID
     * 始终通过流程实例的 startUser 查找其所属部门
     *
     * @param event        节点事件
     * @param isParentDept 是否取上级部门
     * @return 部门ID
     */
    private Long resolveApplicantDeptId(NodeEnteredEvent event, boolean isParentDept) {
        try {
            ProcessInstance instance = processInstanceMapper.selectById(event.getProcessInstanceId());
            if (instance == null || !StringUtils.hasText(instance.getStartUser())) {
                log.warn("[UserTaskNodeHandler] 流程实例或发起人为空, instanceId={}", event.getProcessInstanceId());
                return null;
            }

            String startUsername = instance.getStartUser();
            User starter = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, startUsername)
                            .last("LIMIT 1"));
            if (starter == null || starter.getDeptId() == null) {
                log.warn("[UserTaskNodeHandler] 发起人 {} 不存在或无部门, instanceId={}",
                        startUsername, event.getProcessInstanceId());
                return null;
            }

            Long starterDeptId = starter.getDeptId();
            log.info("[UserTaskNodeHandler] 发起人={}, 部门ID={}", startUsername, starterDeptId);

            if (isParentDept) {
                return getParentDeptId(starterDeptId);
            }
            return starterDeptId;
        } catch (Exception e) {
            log.warn("[UserTaskNodeHandler] 解析部门ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取父部门ID
     */
    private Long getParentDeptId(Long deptId) {
        try {
            Dept dept = deptService.getDept(deptId);
            if (dept.getParentId() != null && dept.getParentId() > 0) {
                return dept.getParentId();
            }
            // 已是顶级部门，返回自身
            return deptId;
        } catch (Exception e) {
            log.warn("[UserTaskNodeHandler] 获取父部门失败, deptId={}: {}", deptId, e.getMessage());
            return deptId;
        }
    }

    /**
     * 解析部门领导的 username
     */
    private String resolveDeptLeaderUsername(Long deptId) {
        try {
            Dept dept = deptService.getDept(deptId);
            if (dept.getLeaderId() != null) {
                User leader = userMapper.selectById(dept.getLeaderId());
                if (leader != null && StringUtils.hasText(leader.getUsername())) {
                    return leader.getUsername();
                }
            }
            // 回退：按 leaderName 查
            if (StringUtils.hasText(dept.getLeaderName())) {
                User user = userMapper.selectOne(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getRealName, dept.getLeaderName())
                                .last("LIMIT 1"));
                if (user != null) {
                    return user.getUsername();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[UserTaskNodeHandler] 查询部门领导失败, deptId={}: {}", deptId, e.getMessage());
            return null;
        }
    }
}
