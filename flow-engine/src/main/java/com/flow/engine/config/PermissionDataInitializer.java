package com.flow.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.*;
import com.flow.engine.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 权限数据初始化器
 * 基于前端菜单和按钮定义，初始化 sys_permission、sys_role_permission、sys_data_permission
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(12)
public class PermissionDataInitializer implements CommandLineRunner {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleMapper roleMapper;
    private final DataPermissionMapper dataPermissionMapper;
    private final DeptMapper deptMapper;

    /** 必须存在的权限 Key，缺失时强制重建 */
    private static final List<String> REQUIRED_PERM_KEYS = Arrays.asList(
            "dashboard", "process", "process:definition", "process:config", "process:designer",
            "process:instance", "form:definition", "form:design", "data-model",
            "task", "task:start", "task:todo", "task:done", "task:my-request",
            "system", "system:dept", "system:user", "system:role", "system:log",
            "system:dict", "system:admin", "monitor"
    );

    @Override
    public void run(String... args) {
        log.info("[PermissionDataInitializer] 开始初始化权限数据...");
        boolean rebuilt = initPermissions();
        initRolePermissions(rebuilt);
        initDataPermissions();
        log.info("[PermissionDataInitializer] 权限数据初始化完成");
    }

    /**
     * 初始化权限数据，返回是否进行了重建
     */
    private boolean initPermissions() {
        Long count = permissionMapper.selectCount(null);
        if (count != null && count > 0) {
            // 检查所有必需 permKey 是否都存在
            boolean allPresent = true;
            for (String key : REQUIRED_PERM_KEYS) {
                Long keyCount = permissionMapper.selectCount(
                        new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, key));
                if (keyCount == null || keyCount == 0) {
                    allPresent = false;
                    log.info("[PermissionDataInitializer] 缺少权限 Key: {}，将强制重建", key);
                    break;
                }
            }
            if (allPresent) {
                log.info("[PermissionDataInitializer] 权限数据已存在且完整，跳过初始化");
                return false;
            }
            // 清除后重新初始化
            log.info("[PermissionDataInitializer] 权限数据不完整，清除后重新初始化...");
            rolePermissionMapper.delete(null);
            permissionMapper.delete(null);
        }

        int sortOrder = 0;

        // ========== 1. 工作台 ==========
        createPermission(0L, "工作台", "dashboard", 1, "dashboard", "/dashboard", ++sortOrder);

        // ========== 2. 流程管理 ==========
        Long processMenuId = createPermission(0L, "流程管理", "process", 1, "process", "/process", ++sortOrder);
        Long processDefId = createPermission(processMenuId, "流程定义", "process:definition", 1, "process", "/process/definition", 1);
        createPermission(processDefId, "新建流程定义", "process:definition:create", 2, "process", null, 1);
        createPermission(processDefId, "编辑流程定义", "process:definition:update", 2, "process", null, 2);
        createPermission(processDefId, "删除流程定义", "process:definition:delete", 2, "process", null, 3);
        createPermission(processDefId, "部署流程定义", "process:definition:deploy", 2, "process", null, 4);
        createPermission(processDefId, "导出流程定义", "process:definition:export", 2, "process", null, 5);

        createPermission(processMenuId, "流程配置", "process:config", 1, "process", "/process/config", 2);
        createPermission(processMenuId, "流程设计器", "process:designer", 1, "process", "/process/designer", 3);

        Long processInstId = createPermission(processMenuId, "流程实例", "process:instance", 1, "process", "/process/instance", 4);
        createPermission(processInstId, "查看流程实例", "process:instance:view", 2, "process", null, 1);
        createPermission(processInstId, "终止流程实例", "process:instance:terminate", 2, "process", null, 2);

        Long formDefId = createPermission(processMenuId, "表单定义", "form:definition", 1, "process", "/form/definition", 5);
        createPermission(formDefId, "新建表单定义", "form:definition:create", 2, "process", null, 1);
        createPermission(formDefId, "编辑表单定义", "form:definition:update", 2, "process", null, 2);
        createPermission(formDefId, "删除表单定义", "form:definition:delete", 2, "process", null, 3);

        createPermission(processMenuId, "表单设计器", "form:design", 1, "process", "/form/design", 6);

        Long dataModelId = createPermission(processMenuId, "数据模型", "data-model", 1, "process", "/data-model", 7);
        createPermission(dataModelId, "新建数据模型", "data-model:create", 2, "process", null, 1);
        createPermission(dataModelId, "编辑数据模型", "data-model:update", 2, "process", null, 2);
        createPermission(dataModelId, "删除数据模型", "data-model:delete", 2, "process", null, 3);
        createPermission(dataModelId, "发布数据模型", "data-model:publish", 2, "process", null, 4);

        // ========== 3. 任务中心 ==========
        Long taskMenuId = createPermission(0L, "任务中心", "task", 1, "task", "/task", ++sortOrder);

        createPermission(taskMenuId, "发起流程", "task:start", 1, "task", "/task/start", 1);

        Long taskTodoId = createPermission(taskMenuId, "待办任务", "task:todo", 1, "task", "/task/todo", 2);
        createPermission(taskTodoId, "签收任务", "task:todo:claim", 2, "task", null, 1);
        createPermission(taskTodoId, "通过任务", "task:todo:complete", 2, "task", null, 2);
        createPermission(taskTodoId, "驳回任务", "task:todo:reject", 2, "task", null, 3);
        createPermission(taskTodoId, "转办任务", "task:todo:transfer", 2, "task", null, 4);
        createPermission(taskTodoId, "委派任务", "task:todo:delegate", 2, "task", null, 5);
        createPermission(taskTodoId, "加签任务", "task:todo:addSign", 2, "task", null, 6);

        createPermission(taskMenuId, "已办任务", "task:done", 1, "task", "/task/done", 3);

        Long myRequestId = createPermission(taskMenuId, "我的申请", "task:my-request", 1, "task", "/task/my-request", 4);
        createPermission(myRequestId, "终止我的申请", "task:my-request:terminate", 2, "task", null, 1);

        // ========== 4. 后台管理 ==========
        Long systemMenuId = createPermission(0L, "后台管理", "system", 1, "system", "/system", ++sortOrder);

        // 部门管理
        Long deptId = createPermission(systemMenuId, "部门管理", "system:dept", 1, "system", "/system/dept", 1);
        createPermission(deptId, "新建部门", "system:dept:create", 2, "system", null, 1);
        createPermission(deptId, "编辑部门", "system:dept:update", 2, "system", null, 2);
        createPermission(deptId, "删除部门", "system:dept:delete", 2, "system", null, 3);
        createPermission(deptId, "添加子部门", "system:dept:add-child", 2, "system", null, 4);

        // 用户管理
        Long userId = createPermission(systemMenuId, "用户管理", "system:user", 1, "system", "/system/user", 2);
        createPermission(userId, "新建用户", "system:user:create", 2, "system", null, 1);
        createPermission(userId, "编辑用户", "system:user:update", 2, "system", null, 2);
        createPermission(userId, "删除用户", "system:user:delete", 2, "system", null, 3);
        createPermission(userId, "重置密码", "system:user:reset-pwd", 2, "system", null, 4);

        // 角色管理
        Long roleId = createPermission(systemMenuId, "角色管理", "system:role", 1, "system", "/system/role", 3);
        createPermission(roleId, "新建角色", "system:role:create", 2, "system", null, 1);
        createPermission(roleId, "编辑角色", "system:role:update", 2, "system", null, 2);
        createPermission(roleId, "删除角色", "system:role:delete", 2, "system", null, 3);
        createPermission(roleId, "权限分配", "system:role:assign-perm", 2, "system", null, 4);

        // 日志管理
        Long logId = createPermission(systemMenuId, "日志管理", "system:log", 1, "system", "/system/log", 4);
        createPermission(logId, "查看访问日志", "system:log:access", 2, "system", null, 1);
        createPermission(logId, "查看操作日志", "system:log:operation", 2, "system", null, 2);
        createPermission(logId, "导出访问日志", "system:log:export-access", 2, "system", null, 3);
        createPermission(logId, "导出操作日志", "system:log:export-operation", 2, "system", null, 4);

        // 数据字典
        Long dictId = createPermission(systemMenuId, "数据字典", "system:dict", 1, "system", "/system/dict", 5);
        createPermission(dictId, "新建字典类型", "system:dict:create-type", 2, "system", null, 1);
        createPermission(dictId, "编辑字典类型", "system:dict:update-type", 2, "system", null, 2);
        createPermission(dictId, "删除字典类型", "system:dict:delete-type", 2, "system", null, 3);
        createPermission(dictId, "新建字典项", "system:dict:create-item", 2, "system", null, 4);
        createPermission(dictId, "编辑字典项", "system:dict:update-item", 2, "system", null, 5);
        createPermission(dictId, "删除字典项", "system:dict:delete-item", 2, "system", null, 6);

        // 三员管理
        Long adminId = createPermission(systemMenuId, "三员管理", "system:admin", 1, "system", "/system/admin", 6);
        createPermission(adminId, "查看三员列表", "system:admin:view", 2, "system", null, 1);
        createPermission(adminId, "查看审计日志", "system:admin:audit", 2, "system", null, 2);

        // ========== 5. 流程监控 ==========
        Long monitorMenuId = createPermission(0L, "流程监控", "monitor", 1, "monitor", "/monitor", ++sortOrder);
        createPermission(monitorMenuId, "查看运行中流程", "monitor:running", 2, "monitor", null, 1);
        createPermission(monitorMenuId, "查看执行轨迹", "monitor:history", 2, "monitor", null, 2);
        createPermission(monitorMenuId, "查看变量历史", "monitor:variables", 2, "monitor", null, 3);
        createPermission(monitorMenuId, "查看耗时统计", "monitor:statistics", 2, "monitor", null, 4);
        createPermission(monitorMenuId, "管理员干预", "monitor:intervene", 2, "monitor", null, 5);
        createPermission(monitorMenuId, "导出监控数据", "monitor:export", 2, "monitor", null, 6);

        return true;
    }

    /**
     * 为三员角色分配权限
     * - system_admin: 所有权限
     * - security_admin: 后台管理 + 日志 + 字典 + 三员管理
     * - audit_admin: 日志管理 + 流程监控（只读）
     *
     * @param forceReassign 是否强制重新分配（权限重建时需要）
     */
    private void initRolePermissions(boolean forceReassign) {
        Role sysAdminRole = getRoleByKey("system_admin");
        Role secAdminRole = getRoleByKey("security_admin");
        Role auditAdminRole = getRoleByKey("audit_admin");

        if (sysAdminRole == null && secAdminRole == null && auditAdminRole == null) {
            log.warn("[PermissionDataInitializer] 三员角色尚未创建，跳过权限分配");
            return;
        }

        List<Permission> allPerms = permissionMapper.selectList(null);
        if (allPerms.isEmpty()) {
            log.warn("[PermissionDataInitializer] 权限数据尚未创建，跳过权限分配");
            return;
        }

        // system_admin: 分配所有权限
        if (sysAdminRole != null) {
            if (forceReassign) {
                rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, sysAdminRole.getId()));
            }
            Long existingCount = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, sysAdminRole.getId()));
            if (existingCount == null || existingCount == 0) {
                for (Permission perm : allPerms) {
                    assignPermission(sysAdminRole.getId(), perm.getId());
                }
                log.info("[PermissionDataInitializer] 系统管理员已分配全部 {} 项权限", allPerms.size());
            }
        }

        // security_admin: 后台管理相关权限
        if (secAdminRole != null) {
            if (forceReassign) {
                rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, secAdminRole.getId()));
            }
            Long existingCount = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, secAdminRole.getId()));
            if (existingCount == null || existingCount == 0) {
                List<Permission> secPerms = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .likeRight(Permission::getPermKey, "system:")
                                .or().eq(Permission::getPermKey, "system"));
                for (Permission perm : secPerms) {
                    assignPermission(secAdminRole.getId(), perm.getId());
                }
                log.info("[PermissionDataInitializer] 安全管理员已分配 {} 项权限", secPerms.size());
            }
        }

        // audit_admin: 日志 + 监控相关权限（只读）
        if (auditAdminRole != null) {
            if (forceReassign) {
                rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, auditAdminRole.getId()));
            }
            Long existingCount = rolePermissionMapper.selectCount(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, auditAdminRole.getId()));
            if (existingCount == null || existingCount == 0) {
                List<Permission> auditPerms = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .likeRight(Permission::getPermKey, "system:log")
                                .or().likeRight(Permission::getPermKey, "monitor")
                                .or().eq(Permission::getPermKey, "dashboard")
                                .or().eq(Permission::getPermKey, "system:admin:audit"));
                for (Permission perm : auditPerms) {
                    assignPermission(auditAdminRole.getId(), perm.getId());
                }
                log.info("[PermissionDataInitializer] 审计管理员已分配 {} 项权限", auditPerms.size());
            }
        }
    }

    /**
     * 初始化数据权限
     * - system_admin: 数据范围=1(全部)
     * - security_admin: 数据范围=2(本部门)
     * - audit_admin: 数据范围=4(仅本人)
     */
    private void initDataPermissions() {
        Role sysAdminRole = getRoleByKey("system_admin");
        Role secAdminRole = getRoleByKey("security_admin");
        Role auditAdminRole = getRoleByKey("audit_admin");

        if (sysAdminRole != null) {
            createDataPermissionIfNotExists(sysAdminRole.getId(), null, 1);
        }
        if (secAdminRole != null) {
            Dept dept = getDefaultDept();
            if (dept != null) {
                createDataPermissionIfNotExists(secAdminRole.getId(), dept.getId(), 2);
            }
        }
        if (auditAdminRole != null) {
            createDataPermissionIfNotExists(auditAdminRole.getId(), null, 4);
        }
    }

    // ========== 工具方法 ==========

    private Long createPermission(Long parentId, String permName, String permKey, Integer permType, String permGroup, String resourcePath, Integer sortOrder) {
        Permission existing = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, permKey));
        if (existing != null) {
            return existing.getId();
        }
        Permission perm = new Permission();
        perm.setParentId(parentId);
        perm.setPermName(permName);
        perm.setPermKey(permKey);
        perm.setPermType(permType);
        perm.setPermGroup(permGroup);
        perm.setResourcePath(resourcePath);
        perm.setSortOrder(sortOrder);
        perm.setCreateTime(LocalDateTime.now());
        permissionMapper.insert(perm);
        return perm.getId();
    }

    private void assignPermission(Long roleId, Long permissionId) {
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rolePermissionMapper.insert(rp);
    }

    private void createDataPermissionIfNotExists(Long roleId, Long deptId, Integer dataScope) {
        Long count = dataPermissionMapper.selectCount(
                new LambdaQueryWrapper<DataPermission>().eq(DataPermission::getRoleId, roleId));
        if (count != null && count > 0) {
            return;
        }
        DataPermission dp = new DataPermission();
        dp.setRoleId(roleId);
        dp.setDeptId(deptId);
        dp.setDataScope(dataScope);
        dataPermissionMapper.insert(dp);
    }

    private Role getRoleByKey(String roleKey) {
        return roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getRoleKey, roleKey));
    }

    private Dept getDefaultDept() {
        List<Dept> depts = deptMapper.selectList(null);
        return depts.isEmpty() ? null : depts.get(0);
    }
}
