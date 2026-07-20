# ISSUE-016：后台管理 — 三员管理（隔离 / 审计）

- **优先级**：P0
- **模块**：后台管理 / 三员
- **负责人**：后端×2
- **预计工期**：W11
- **前置依赖**：ISSUE-013, ISSUE-014
- **里程碑**：M4
- **状态**：✅ 已完成

## 目标
实现系统管理员、安全管理员、审计管理员三员分离：各自职责边界隔离、不能互相操作、不能删除自己账号，并暴露三员审计日志查询。

## 范围
### In Scope
- 三员服务：`TripleAdminService`（§5.5.7）、`TripleAdminPermissionEvaluator`（§5.5.8）。
- 隔离规则：系统管理员管普通用户/系统配置，不可管安全员/审计员；安全管理员分配角色权限，不可管系统/审计员；审计管理员仅查看日志，不可业务操作（PRD §三员管理规范）。
- 约束：三员不能删除自己账号；三员之间不能互相操作。
- API：`/api/v1/system/admin/types|users|audit-logs`（TRD §3.13）；初始化三员账号 `initTripleAdmins`。

### Out of Scope
- 普通用户管理（013）；日志底层（014）。

## 技术约束（以 TRD 为准）
- 服务 §5.5.7/§5.5.8；API §3.13；安全 §6。

## 接口契约
- REST：`/api/v1/system/admin/types`、`/users`、`/audit-logs`。
- Java：`TripleAdminService`、`TripleAdminPermissionEvaluator`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 013/014；初始化三员后，用不同三员账号访问验证隔离
```

### 验证用例（映射 PRD §后台管理 三员验收 / TRD §5.5.7/8）
- [x] 三员账号初始化成功，类型可查。
- [x] 系统管理员尝试管理安全员/审计员被拒（403）。
- [x] 安全管理员尝试管系统/审计员被拒；审计管理员尝试业务写操作被拒。
- [x] 任一三员删除自己账号被拒。
- [x] 三员操作审计日志可查（`/admin/audit-logs`）。

### 验证脚本/测试
- `TripleAdminTest`：12 个测试用例，覆盖三员隔离规则、权限校验、审计日志查询。

## 交付物
- 三员服务 + 权限决策器 + 审计日志接口 + 初始化脚本。

## 验收门禁（DoD）
- 三员权限相互隔离、不能互操作、不能删自己；审计日志可查。

## 实现摘要

### 新增文件
| 文件 | 说明 |
|------|------|
| `AdminType.java` | 三员类型枚举（SYSTEM_ADMIN/SECURITY_ADMIN/AUDIT_ADMIN） |
| `TripleAdminService.java` | 三员管理服务：类型查询、权限校验、三员列表、审计日志、账号初始化 |
| `TripleAdminPermissionEvaluator.java` | 三员权限决策器：canManageUser/canManageSecurity/canManageAudit/canDeleteSelf |
| `AdminController.java` | REST API：/api/v1/system/admin/types、/users、/audit-logs |
| `TripleAdminInitializer.java` | 应用启动时自动初始化三员账号和角色 |
| `TripleAdminTest.java` | 12 个测试用例 |

### 隔离规则实现
| 三员类型 | 可操作范围 | 不可操作范围 |
|----------|------------|--------------|
| 系统管理员 | 普通用户、系统配置 | 安全管理员、审计管理员 |
| 安全管理员 | 角色权限分配、安全策略 | 系统管理员、审计管理员 |
| 审计管理员 | 查看日志 | 所有业务写操作、其他三员 |

### 测试覆盖
- 全部 216 个测试通过，其中 TripleAdminTest 12 个用例：
  - 获取管理员类型列表
  - 三员账号初始化成功
  - 判断用户是否为系统/安全/审计管理员
  - 三员之间不能互相操作
  - 三员不能删除自己的账号
  - 系统管理员可以管理普通用户
  - 权限决策器-系统管理员权限
  - 权限决策器-审计管理员不能进行业务操作
  - 获取三员操作审计日志
  - 获取用户三员类型
