# ISSUE-009：表单权限 — 节点/字段/按钮/条件/继承覆盖

- **优先级**：P0
- **模块**：表单引擎 / 权限
- **负责人**：后端×2 + 前端×1
- **预计工期**：W9
- **前置依赖**：ISSUE-008, ISSUE-005
- **里程碑**：M3
- **状态**：✅ 已完成

## 目标
按流程节点计算表单的操作权限：节点级（edit/readonly/hidden）、字段级、按钮级，并支持条件化权限与权限的继承/覆盖（子节点覆盖父配置）。

## 范围
### In Scope
- 权限计算器：`PermissionCalculator.calculatePermissions / mergePermissions / evaluateCondition`（TRD §4.5.1）。
- 接口：`GET /api/v1/tasks/{taskId}/form-permissions`（TRD §3.4）。
- 维度：节点级（edit/readonly/hidden）、字段级、按钮级、条件化（基于变量）、继承与覆盖（PRD §表单操作权限）。
- 权限配置绑定到流程节点（PRD §约束）。

### Out of Scope
- 功能/数据权限（013）；三员（016）。

## 技术约束（以 TRD 为准）
- 设计见 TRD §4.5.1；权限来源可存于流程定义 JSON 的节点配置。

## 接口契约
- REST：`GET /api/v1/tasks/{taskId}/form-permissions`。
- Java：`PermissionCalculator`、`FormPermissionService`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 008/005；用任务 + 节点权限配置调用权限计算接口验证各维度
```

### 验证用例（映射 PRD §表单引擎 权限验收 / TRD §4.5.1）
- [x] 节点级：某节点表单为 readonly，返回 readonly。
- [x] 字段级：特定字段 hidden/edit/readonly 正确。
- [x] 按钮级：提交/驳回按钮按配置显隐。
- [x] 条件化：`${amount>1000}` 时某字段变为只读，否则可编辑。
- [x] 继承覆盖：子节点未配字段权限时继承父；显式配置覆盖父。

### 验证脚本/测试
- `PermissionCalculatorTest`：合并/条件化/继承覆盖用例。
- `FormPermissionApiTest`（MockMvc）：不同任务返回不同权限。

## 交付物
- 表单权限计算器 + 接口 + 与节点配置绑定。

## 验收门禁（DoD）
- 节点/字段/按钮级权限正确；条件化与继承覆盖生效；接口按任务返回权限。

## 测试结果
- 测试用例：21 个（PermissionCalculatorTest 14个 + FormPermissionApiTest 7个）
- 全部通过：95 tests, 0 failures, 0 errors
