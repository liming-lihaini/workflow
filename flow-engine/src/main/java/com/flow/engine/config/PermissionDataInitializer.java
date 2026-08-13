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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限数据初始化器
 * <p>
 * 基于前端页面（BasicLayout 菜单 + 各页面 hasPerm 按钮）全量重建操作权限数据表：
 * - sys_permission：菜单（permType=1）、按钮（permType=2）、数据权限（permType=3）
 * - sys_role_permission：三员角色权限分配
 * - sys_data_permission：三员角色数据范围
 * <p>
 * 每次启动幂等同步静态权限（按 permKey 保 ID 更新、缺失新增、清单外删除），
 * 保证权限 ID 稳定，运行期通过「角色管理-权限分配」授予的 sys_role_permission 绑定跨重启保留；
 * 运行时动态产生的 model-data:* 权限（数据模型生成表时创建）及其角色绑定予以保留。
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

    /** 数据权限节点 Key 后缀：查看模块全部数据 */
    private static final String DATA_ALL_SUFFIX = ":data-all";

    /** 动态数据模型权限 Key 前缀 */
    private static final String MODEL_DATA_PREFIX = "model-data";

    /** 本次同步已声明的静态权限 Key（用于清单外清理） */
    private final Set<String> declaredPermKeys = new java.util.LinkedHashSet<>();

    /** 需要挂载「查看模块全部数据」数据权限的业务模块菜单 Key */
    private static final List<String> DATA_SCOPE_MODULES = Arrays.asList(
            "process:definition", "process:instance", "form:definition", "data-model",
            "task:todo", "task:done", "task:my-request",
            "ems:customer", "ems:entrust", "ems:contract", "ems:dispatch", "ems:sample",
            "ems:detection", "ems:quality", "ems:report", "ems:dashboard",
            "ems:vehicle", "ems:instrument"
    );

    @Override
    public void run(String... args) {
        log.info("[PermissionDataInitializer] 开始同步权限数据...");
        initPermissions();
        initRolePermissions();
        initDataPermissions();
        log.info("[PermissionDataInitializer] 权限数据同步完成，共 {} 项权限",
                permissionMapper.selectCount(null));
    }

    /**
     * 幂等同步 sys_permission 静态权限：
     * - 已存在（按 permKey）：保留 ID 更新属性，保障 sys_role_permission 绑定不失效
     * - 不存在：新增
     * - 清单外且非动态 model-data 权限：删除并清理对应角色绑定
     */
    private void initPermissions() {
        declaredPermKeys.clear();

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
        createPermission(dataModelId, "生成数据表", "data-model:generate", 2, "process", null, 5);

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

        createPermission(taskMenuId, "委托与代理", "task:delegation", 1, "task", "/task/delegation", 5);
        createPermission(taskMenuId, "创建全局委托", "task:delegation:create", 2, "task", null, 1);
        createPermission(taskMenuId, "取消全局委托", "task:delegation:cancel", 2, "task", null, 2);
        createPermission(taskMenuId, "代理记录", "task:proxy", 1, "task", "/task/proxy", 6);

        // ========== 4. 后台管理 ==========
        Long systemMenuId = createPermission(0L, "后台管理", "system", 1, "system", "/system", ++sortOrder);

        Long deptId = createPermission(systemMenuId, "部门管理", "system:dept", 1, "system", "/system/dept", 1);
        createPermission(deptId, "新建部门", "system:dept:create", 2, "system", null, 1);
        createPermission(deptId, "编辑部门", "system:dept:update", 2, "system", null, 2);
        createPermission(deptId, "删除部门", "system:dept:delete", 2, "system", null, 3);
        createPermission(deptId, "添加子部门", "system:dept:add-child", 2, "system", null, 4);

        Long userId = createPermission(systemMenuId, "用户管理", "system:user", 1, "system", "/system/user", 2);
        createPermission(userId, "新建用户", "system:user:create", 2, "system", null, 1);
        createPermission(userId, "编辑用户", "system:user:update", 2, "system", null, 2);
        createPermission(userId, "删除用户", "system:user:delete", 2, "system", null, 3);
        createPermission(userId, "重置密码", "system:user:reset-pwd", 2, "system", null, 4);
        createPermission(userId, "授权角色", "system:user:assign-role", 2, "system", null, 5);

        Long roleId = createPermission(systemMenuId, "角色管理", "system:role", 1, "system", "/system/role", 3);
        createPermission(roleId, "新建角色", "system:role:create", 2, "system", null, 1);
        createPermission(roleId, "编辑角色", "system:role:update", 2, "system", null, 2);
        createPermission(roleId, "删除角色", "system:role:delete", 2, "system", null, 3);
        createPermission(roleId, "权限分配", "system:role:assign-perm", 2, "system", null, 4);

        Long logId = createPermission(systemMenuId, "日志管理", "system:log", 1, "system", "/system/log", 4);
        createPermission(logId, "查看访问日志", "system:log:access", 2, "system", null, 1);
        createPermission(logId, "查看操作日志", "system:log:operation", 2, "system", null, 2);
        createPermission(logId, "导出访问日志", "system:log:export-access", 2, "system", null, 3);
        createPermission(logId, "导出操作日志", "system:log:export-operation", 2, "system", null, 4);

        Long dictId = createPermission(systemMenuId, "数据字典", "system:dict", 1, "system", "/system/dict", 5);
        createPermission(dictId, "新建字典类型", "system:dict:create-type", 2, "system", null, 1);
        createPermission(dictId, "编辑字典类型", "system:dict:update-type", 2, "system", null, 2);
        createPermission(dictId, "删除字典类型", "system:dict:delete-type", 2, "system", null, 3);
        createPermission(dictId, "新建字典项", "system:dict:create-item", 2, "system", null, 4);
        createPermission(dictId, "编辑字典项", "system:dict:update-item", 2, "system", null, 5);
        createPermission(dictId, "删除字典项", "system:dict:delete-item", 2, "system", null, 6);

        Long adminId = createPermission(systemMenuId, "三员管理", "system:admin", 1, "system", "/system/admin", 6);
        createPermission(adminId, "查看三员列表", "system:admin:view", 2, "system", null, 1);
        createPermission(adminId, "查看审计日志", "system:admin:audit", 2, "system", null, 2);

        // ========== 5. 环境检测（环境监测 LIMS）==========
        Long emsMenuId = createPermission(0L, "环境检测", "ems", 1, "ems", "/ems", ++sortOrder);

        Long customerId = createPermission(emsMenuId, "客户管理", "ems:customer", 1, "ems", "/ems/base/customer", 1);
        createPermission(customerId, "新建客户", "ems:customer:create", 2, "ems", null, 1);
        createPermission(customerId, "编辑客户/启用停用", "ems:customer:update", 2, "ems", null, 2);
        createPermission(customerId, "删除客户", "ems:customer:delete", 2, "ems", null, 3);
        createPermission(customerId, "导入客户", "ems:customer:import", 2, "ems", null, 4);

        Long entrustId = createPermission(emsMenuId, "检测委托", "ems:entrust", 1, "ems", "/ems/base/entrust", 2);
        createPermission(entrustId, "新建委托", "ems:entrust:create", 2, "ems", null, 1);
        createPermission(entrustId, "委托编辑/提交/确认/退回", "ems:entrust:update", 2, "ems", null, 2);
        createPermission(entrustId, "删除委托", "ems:entrust:delete", 2, "ems", null, 3);

        Long contractId = createPermission(emsMenuId, "合同台账", "ems:contract", 1, "ems", "/ems/contract", 3);
        createPermission(contractId, "合同新建/编辑/状态操作", "ems:contract:edit", 2, "ems", null, 1);
        createPermission(contractId, "收款/支付登记与撤销", "ems:contract:finance", 2, "ems", null, 2);
        createPermission(contractId, "删除草稿合同/收付款登记", "ems:contract:delete", 2, "ems", null, 3);

        Long dispatchId = createPermission(emsMenuId, "采样任务", "ems:dispatch", 1, "ems", "/ems/base/dispatch", 4);
        createPermission(dispatchId, "派单", "ems:dispatch:assign", 2, "ems", null, 1);
        createPermission(dispatchId, "采样任务编辑/完成", "ems:dispatch:update", 2, "ems", null, 2);
        createPermission(dispatchId, "删除采样任务", "ems:dispatch:delete", 2, "ems", null, 3);

        Long sampleId = createPermission(emsMenuId, "样品管理", "ems:sample", 1, "ems", "/ems/base/sample", 5);
        createPermission(sampleId, "样品登记", "ems:sample:create", 2, "ems", null, 1);
        createPermission(sampleId, "样品处置/领用/解绑", "ems:sample:update", 2, "ems", null, 2);
        createPermission(sampleId, "删除样品/留样记录", "ems:sample:delete", 2, "ems", null, 3);

        Long detectionId = createPermission(emsMenuId, "检测管理", "ems:detection", 1, "ems", "/ems/base/data-entry", 6);
        createPermission(detectionId, "新建检测任务", "ems:detection:create", 2, "ems", null, 1);
        createPermission(detectionId, "检测结果录入", "ems:detection:entry", 2, "ems", null, 2);
        createPermission(detectionId, "检测结果复核", "ems:detection:review", 2, "ems", null, 3);

        Long qualityId = createPermission(emsMenuId, "质控计划", "ems:quality", 1, "ems", "/ems/quality/plan", 7);
        createPermission(qualityId, "新建质控计划/物资", "ems:quality:create", 2, "ems", null, 1);
        createPermission(qualityId, "质控编辑/提交/活动维护", "ems:quality:update", 2, "ems", null, 2);
        createPermission(qualityId, "质控审批/完成", "ems:quality:approve", 2, "ems", null, 3);
        createPermission(qualityId, "删除质控计划", "ems:quality:delete", 2, "ems", null, 4);

        createPermission(emsMenuId, "监测驾驶舱", "ems:dashboard", 1, "ems", "/ems/dashboard", 8);

        Long reportId = createPermission(emsMenuId, "报告审核", "ems:report", 1, "ems", "/ems/report/review", 9);
        createPermission(reportId, "生成报告", "ems:report:create", 2, "ems", null, 1);
        createPermission(reportId, "提交报告审核", "ems:report:submit", 2, "ems", null, 2);
        createPermission(reportId, "报告审批通过/退回", "ems:report:audit", 2, "ems", null, 3);

        // ========== 6. 流程监控 ==========
        Long monitorMenuId = createPermission(0L, "流程监控", "monitor", 1, "monitor", "/monitor", ++sortOrder);
        createPermission(monitorMenuId, "查看运行中流程", "monitor:running", 2, "monitor", null, 1);
        createPermission(monitorMenuId, "查看执行轨迹", "monitor:history", 2, "monitor", null, 2);
        createPermission(monitorMenuId, "查看变量历史", "monitor:variables", 2, "monitor", null, 3);
        createPermission(monitorMenuId, "查看耗时统计", "monitor:statistics", 2, "monitor", null, 4);
        createPermission(monitorMenuId, "管理员干预", "monitor:intervene", 2, "monitor", null, 5);
        createPermission(monitorMenuId, "导出监控数据", "monitor:export", 2, "monitor", null, 6);

        // ========== 7. 资源管理 ==========
        // 「采样参数配置」页面复用分组 Key ems:base 进行权限控制，不再单独建行
        Long emsBaseMenuId = createPermission(0L, "资源管理", "ems:base", 1, "ems", "/ems/base", ++sortOrder);
        createPermission(emsBaseMenuId, "采样参数配置维护", "ems:base:update", 2, "ems", null, 1);
        createPermission(emsBaseMenuId, "采样参数配置删除", "ems:base:delete", 2, "ems", null, 2);

        Long vehicleId = createPermission(emsBaseMenuId, "车辆台账", "ems:vehicle", 1, "ems", "/ems/base/vehicle", 3);
        createPermission(vehicleId, "新建车辆", "ems:vehicle:create", 2, "ems", null, 1);
        createPermission(vehicleId, "车辆编辑/维修保养", "ems:vehicle:update", 2, "ems", null, 2);
        createPermission(vehicleId, "删除车辆", "ems:vehicle:delete", 2, "ems", null, 3);

        Long instrumentId = createPermission(emsBaseMenuId, "物资管理", "ems:instrument", 1, "ems", "/ems/base/asset-manage", 4);
        createPermission(instrumentId, "新建设备/物资", "ems:instrument:create", 2, "ems", null, 1);
        createPermission(instrumentId, "设备物资编辑/校准/出入库/报废", "ems:instrument:update", 2, "ems", null, 2);
        createPermission(instrumentId, "删除设备", "ems:instrument:delete", 2, "ems", null, 3);

        // ========== 8. 模块数据权限：查看模块全部数据 ==========
        createModuleDataPermissions();

        // 清理清单外且非动态权限的残留记录（连同角色绑定），保证 ID 不变的前提下清单收敛
        List<Long> removedIds = permissionMapper.selectList(null).stream()
                .filter(p -> !declaredPermKeys.contains(p.getPermKey())
                        && !p.getPermKey().startsWith(MODEL_DATA_PREFIX))
                .map(Permission::getId)
                .collect(Collectors.toList());
        if (!removedIds.isEmpty()) {
            permissionMapper.deleteBatchIds(removedIds);
            rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                    .in(RolePermission::getPermissionId, removedIds));
            log.info("[PermissionDataInitializer] 清理清单外残留权限 {} 项", removedIds.size());
        }
    }

    /**
     * 为业务模块挂载「查看模块全部数据」数据权限节点（permType=3）。
     * 持有该权限的用户可查看对应模块的全部数据（跨部门/跨创建人）。
     */
    private void createModuleDataPermissions() {
        for (String moduleKey : DATA_SCOPE_MODULES) {
            Permission module = permissionMapper.selectOne(
                    new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, moduleKey));
            if (module == null) {
                log.warn("[PermissionDataInitializer] 模块菜单不存在，跳过数据权限: {}", moduleKey);
                continue;
            }
            createPermission(module.getId(), "查看模块全部数据", moduleKey + DATA_ALL_SUFFIX,
                    3, module.getPermGroup(), null, 99);
        }
    }

    /**
     * 为三员角色分配权限
     * - system_admin: 所有权限
     * - security_admin: 后台管理相关权限
     * - audit_admin: 日志管理 + 流程监控（只读）
     */
    private void initRolePermissions() {
        List<Permission> allPerms = permissionMapper.selectList(null);
        if (allPerms.isEmpty()) {
            log.warn("[PermissionDataInitializer] 权限数据尚未创建，跳过权限分配");
            return;
        }

        // system_admin: 分配所有权限（含动态 model-data 权限）
        Role sysAdminRole = getRoleByKey("system_admin");
        if (sysAdminRole != null) {
            Set<Long> assigned = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, sysAdminRole.getId()))
                    .stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
            for (Permission perm : allPerms) {
                if (!assigned.contains(perm.getId())) {
                    assignPermission(sysAdminRole.getId(), perm.getId());
                }
            }
            log.info("[PermissionDataInitializer] 系统管理员权限分配完成，权限总数 {}", allPerms.size());
        }

        // security_admin: 后台管理相关权限
        Role secAdminRole = getRoleByKey("security_admin");
        if (secAdminRole != null) {
            List<Permission> secPerms = permissionMapper.selectList(
                    new LambdaQueryWrapper<Permission>()
                            .likeRight(Permission::getPermKey, "system:")
                            .or().eq(Permission::getPermKey, "system"));
            Set<Long> assigned = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, secAdminRole.getId()))
                    .stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
            for (Permission perm : secPerms) {
                if (!assigned.contains(perm.getId())) {
                    assignPermission(secAdminRole.getId(), perm.getId());
                }
            }
            log.info("[PermissionDataInitializer] 安全管理员权限分配完成，共 {} 项", secPerms.size());
        }

        // audit_admin: 日志 + 监控相关权限（只读）
        Role auditAdminRole = getRoleByKey("audit_admin");
        if (auditAdminRole != null) {
            List<Permission> auditPerms = permissionMapper.selectList(
                    new LambdaQueryWrapper<Permission>()
                            .likeRight(Permission::getPermKey, "system:log")
                            .or().likeRight(Permission::getPermKey, "monitor")
                            .or().eq(Permission::getPermKey, "dashboard")
                            .or().eq(Permission::getPermKey, "system:admin:audit"));
            Set<Long> assigned = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                            .eq(RolePermission::getRoleId, auditAdminRole.getId()))
                    .stream().map(RolePermission::getPermissionId).collect(Collectors.toSet());
            for (Permission perm : auditPerms) {
                if (!assigned.contains(perm.getId())) {
                    assignPermission(auditAdminRole.getId(), perm.getId());
                }
            }
            log.info("[PermissionDataInitializer] 审计管理员权限分配完成，共 {} 项", auditPerms.size());
        }
    }

    /**
     * 初始化数据权限（角色级数据范围）
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
        declaredPermKeys.add(permKey);
        Permission existing = permissionMapper.selectOne(
                new LambdaQueryWrapper<Permission>().eq(Permission::getPermKey, permKey));
        if (existing != null) {
            // 保留原 ID 更新属性，避免角色授权绑定因 ID 变化而失效
            existing.setParentId(parentId);
            existing.setPermName(permName);
            existing.setPermType(permType);
            existing.setPermGroup(permGroup);
            existing.setResourcePath(resourcePath);
            existing.setSortOrder(sortOrder);
            permissionMapper.updateById(existing);
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
