# ISSUE-013：后台管理 — 部门/用户/角色/权限

- **优先级**：P0
- **状态**：✅ 已完成（2026-07-15）
- **模块**：后台管理
- **负责人**：后端×2 + 前端×1
- **预计工期**：W10
- **前置依赖**：ISSUE-001
- **里程碑**：M4

## 目标
实现基础组织与权限底座：部门树、用户账号（密级/兼职）、角色、功能权限与数据权限，并提供权限决策器供全局鉴权（Spring Security 过滤器，TRD §6.1）。为三员（016）、日志（014）提供基础数据。

## 范围
### In Scope
- 部门：`sys_dept`（§2.1.8）；树形 CRUD、设置领导（§5.5.1）。
- 用户：`sys_user`/`sys_user_post`（§2.1.9/10）；CRUD、密级、兼职（§5.5.2）。
- 角色：`sys_role`/`sys_user_role`/`sys_permission`/`sys_role_permission`/`sys_data_permission`（§2.1.11~15）；角色/权限分配、数据权限范围（§5.5.3）。
- 权限决策：`PermissionEvaluator.hasPermission/canAccessData/checkSecurityLevel`（§5.5.4）。
- API：`/api/v1/system/depts|users|roles|permissions/*`（TRD §3.7~3.10）。
- 认证：JWT + 内存会话；Spring Security 自定义过滤器（TRD §6.1）。

### Out of Scope
- 日志审计（014）；三员（016）；字典（015）。

## 技术约束（以 TRD 为准）
- 表：`sys_*` 共 12 张（§2.1.8~15）。
- API §3.7~3.10；安全设计 §6。

## 接口契约
- REST：`/api/v1/system/depts|users|roles|permissions/*`（见 TRD §3.7~3.10）。
- Java：`DeptService`、`UserService`、`RolePermissionService`、`PermissionEvaluator`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 本 Issue 不依赖业务模块，可独立 CRUD 组织/用户/角色/权限并验证鉴权
```

### 验证用例（映射 PRD §后台管理 验收标准 / TRD §5.5）
- [x] 部门树形展示、CRUD；顶级部门唯一；删除部门需转移用户。
- [x] 用户 CRUD + 密级设置；兼职多部门（主部门唯一）；获取用户可访问部门含兼职。
- [x] 角色 CRUD、分配用户、分配权限；数据权限范围（全部/部门/部门及子部门/本人）。
- [x] 权限决策：无权限用户访问受保护接口返回 403；有权限返回 200。
- [x] 密级校验：低密级用户访问高密级数据被拒。

### 验证脚本/测试
- `DeptServiceTest`：树形/领导/删除转移。
- `UserServiceTest`：兼职/可访问部门。
- `PermissionEvaluatorTest`：功能/数据权限/密级。
- `SecurityFilterApiTest`（MockMvc）：带/不带 JWT 访问受保护资源。

## 交付物
- 组织/用户/角色/权限全套服务 + 鉴权过滤器 + 决策器。

## 验收门禁（DoD）
- [x] 部门树、用户（含兼职/密级）、角色权限、数据权限均可用；无权限访问被拒（403）。

## 实现摘要
- 8 个实体 + 8 个 Mapper（Dept/User/UserPost/Role/UserRole/Permission/RolePermission/DataPermission）
- 5 个核心服务：DeptService、UserService、RolePermissionService、PermissionEvaluator、AuthService
- 5 个 REST Controller：DeptController、UserController、RoleController、PermissionController、AuthController
- Token + ConcurrentHashMap 内存会话认证（60 分钟有效期）
- SHA-256 密码哈希
- 4 种数据权限范围：全部/本部门/本部门及子部门/仅本人
- 4 级密级：公开/内部/秘密/机密

## 测试覆盖
- DeptServiceTest（6 用例）、UserServiceTest（5 用例）、PermissionEvaluatorTest（5 用例）、SecurityFilterApiTest（8 用例）
- 全部 154 个测试通过，BUILD SUCCESS
