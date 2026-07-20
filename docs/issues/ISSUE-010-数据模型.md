# ISSUE-010：数据模型 — 主子表 / 计算字段 / 绑定 / 流程集成

- **状态**：已完成
- **优先级**：P1
- **模块**：数据模型
- **负责人**：后端×2
- **预计工期**：W8
- **前置依赖**：ISSUE-008, ISSUE-003
- **里程碑**：M3

## 目标
支持定义复杂业务数据结构：主表 + 多个子表（一对多）、计算字段与自动汇总，并支持表单绑定数据模型自动生成字段，以及流程发起/结束时的模型实例生命周期管理。

## 范围
### In Scope
- 模型定义：`wf_data_model`（§2.1.5）、`wf_model_instance`（§2.1.6）；CRUD/publish（TRD §3.5）。
- `DataModelParser.parse / validate`、`ModelInstanceManager.create/update/getInstance/validate`（TRD §4.3.1/4.3.2）。
- 计算字段：`computeField`（TRD §4.3.1）；主子表动态增删行。
- 表单绑定模型：自动生成表单字段（PRD §表单绑定数据模型）。
- 流程集成：发起时自动创建模型实例、节点读取模型数据、结束归档（PRD §数据模型与流程集成）。
- 约束：单模型子表 ≤10、单子表字段 ≤50（PRD §约束）；model_key 全局唯一。

### Out of Scope
- 表单权限（009）；复杂历史兼容（PRD 风险2，留待评估）。

## 技术约束（以 TRD 为准）
- 表：`wf_data_model`/`wf_model_instance`（§2.1.5/6）；缓存 `model:def:{modelKey}`（§5.4）。
- API `/api/v1/data-models/*`（TRD §3.5）。

## 接口契约
- REST：`/api/v1/data-models`(GET/POST)、`/{modelKey}`(GET/PUT/DELETE)、`/{modelKey}/publish`(POST)、`/instances`(POST)、`/instances/{id}`(GET/PUT)。
- Java：`DataModelParser`、`ModelInstanceManager`、`DataModelService`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 008(表单)/003(流程定义)；数据模型自身可独立 CRUD 与计算字段验证
```

### 验证用例（映射 PRD §数据模型 验收标准 / TRD §4.3）
- [x] 创建含主表 + 2 子表的数据模型，子表支持动态增删行。
- [x] 计算字段：如 `合计=单价*数量`，更新明细后自动重算。
- [x] 表单绑定模型：自动生成对应表单字段并映射。
- [x] 流程发起时自动创建模型实例；结束自动归档（与 004 联调）。
- [x] 子表数 >10 或字段 >50 时校验失败。

### 验证脚本/测试
- `DataModelParserTest`：主子表解析与校验。
- `ComputedFieldTest`：计算字段自动汇总。
- `ModelInstanceIntegrationTest`：流程发起→实例创建→归档。

## 交付物
- 数据模型定义/实例管理 + 解析/校验/计算字段 + 表单绑定 + 流程集成。

## 验收门禁（DoD）
- 主子表与计算字段可用；表单绑定自动生成字段；流程集成（发起建实例/结束归档）生效。

## 测试结果
- 测试用例总数：29 个（DataModelParserTest 12 + ComputedFieldTest 7 + ModelInstanceIntegrationTest 10）
- 全部 124 个测试通过（含历史测试）
