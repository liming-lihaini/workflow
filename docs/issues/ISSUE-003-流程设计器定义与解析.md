# ISSUE-003：流程设计器 — 定义 CRUD / 版本 / 导入导出 + JSON 解析器 ✅ 已完成

- **优先级**：P0
- **模块**：流程设计器
- **负责人**：后端×2
- **预计工期**：W4
- **前置依赖**：ISSUE-002
- **里程碑**：M2
- **状态**：✅ 已完成（2026-07-15）

## 目标
提供流程定义的持久化与解析能力：通过 JSON 创建/部署流程定义，支持版本管理、导入导出；并交付 `ProcessJsonParser`，将 JSON 解析为节点/连线/网关/事件模型，供运行引擎（004）消费。

## 范围
### In Scope
- 流程定义 CRUD：`POST /api/v1/process/definitions`、`GET` 列表/详情、`PUT`、`DELETE`、`{id}/deploy`（TRD §3.1）。
- 版本管理：同名 `process_key` 多次保存生成递增 `version`（TRD §2.1.1 status 0-草稿/1-已部署）。
- 导入导出：`{id}/export` 返回 JSON；`/import` 接收 JSON 重建定义（PRD §流程设计器 导入导出）。
- `ProcessJsonParser`：`parse(json)`、`getNextNode(def,currentNodeId,variables)`、`evaluateCondition(condition,variables)`（TRD §4.1.3）。
- 校验：必须包含 start 与 end 节点（PRD §约束）；模板名全局唯一；单次节点数 ≤ 500（PRD §性能约束）。

### Out of Scope
- 实际执行流转（ISSUE-004）。
- 可视化拖拽 UI（ISSUE-019）。

## 技术约束（以 TRD 为准）
- 表：`wf_process_definition`（TRD §2.1.1）；ORM MyBatis-Plus。
- API 前缀 `/api/v1`（TRD §3.1）。
- 节点类型标识全局唯一（PRD §节点插件）。

## 接口契约
- REST：`/api/v1/process/definitions` (GET/POST)、`/{id}` (GET/PUT/DELETE)、`/{id}/deploy` (POST)、`/{id}/export` (GET)、`/import` (POST)。
- Java：`ProcessJsonParser.parse/getNextNode/evaluateCondition`；`ProcessDefinitionService` 接口（create/publish/getByKey/list）。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 002 的节点框架；运行引擎未实现，本 Issue 仅验证“定义可存/可解析/可导出”
```

### 验证用例（映射 PRD §流程设计器 验收标准 / TRD §4.1.3）
- [x] 通过 JSON 创建流程定义，`process_key` 唯一校验生效（重复报 400）。
- [x] 同一 key 二次保存生成 version=2；`deploy` 后 status=1（已部署）。
- [x] `export` 导出 JSON，再 `import` 回来，节点/连线数量一致（往返一致）。
- [x] 解析一段请假流程 JSON，断言节点数、start→userTask→end 连线正确。
- [x] 缺失 start 或 end 节点时校验失败（返回明确错误）。
- [x] `evaluateCondition("${days>3}", {days:5})` = true；`getNextNode` 在条件分支下按变量选对分支。

### 验证脚本/测试
- `ProcessDefinitionApiTest`（MockMvc）：CRUD + deploy + import/export 往返。
- `ProcessJsonParserTest`：解析、条件分支、下一节点选择。

## 交付物
- 流程定义后端 + 解析器 + 导入导出 + 版本校验。

## 验收门禁（DoD）
- 可通过 JSON 建流程；版本管理正确；导入导出往返一致；解析器能正确解析节点/连线/条件分支。
