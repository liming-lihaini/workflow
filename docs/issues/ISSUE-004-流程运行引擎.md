# ISSUE-004：流程运行引擎 — 实例 / 节点执行 / 变量 / 状态机 ✅ 已完成

- **优先级**：P0
- **模块**：流程运行引擎
- **负责人**：后端×2
- **预计工期**：W5
- **前置依赖**：ISSUE-003
- **里程碑**：M2
- **状态**：✅ 已完成（2026-07-15）

## 目标
实现引擎驱动：发起流程实例、按解析器推进节点（顺序/条件/并行/回退），管理流程变量（EL 访问、作用域）与实例状态机（草稿→运行→完成/暂停/恢复/终止），使一条带 userTask 的流程端到端跑通。

## 范围
### In Scope
- 实例管理：`POST /api/v1/process/instances`（发起）、`suspend/resume/terminate`、`{id}/variables` GET/PUT（TRD §3.2）。
- 节点执行：`NodeExecutor` 调用 `NodeHandler.onEnter/execute/onLeave` 并推进（TRD §4.1.1 `executeNode`）；支持顺序、条件（exclusiveGateway）、并行（parallelGateway 汇聚）、回退（`rollback`）。
- 流程变量：流程级/节点级，`${var}` EL 访问；变量名不以 `$` 开头（PRD §流程变量）。
- 状态机：草稿→运行→完成，暂停/恢复/终止（终止不可逆，TRD §5.1）。
- 并发安全：本地锁 + 乐观锁（TRD §5.2）。
- 缓存：Caffeine 缓存 `process:def:{key}:{version}`、`process:instance:{id}`（TRD §5.4）。

### Out of Scope
- 任务中心业务逻辑（ISSUE-005，userTask 节点在此仅创建任务占位）。
- 会签/加签（006/007）、Webhook（012）、监控（017）。

## 技术约束（以 TRD 为准）
- 表：`wf_process_instance`（TRD §2.1.2）、`wf_variable`（§2.1.7）。
- API `/api/v1/process/instances` 系列（TRD §3.2）。
- 缓存键与过期见 TRD §5.4。

## 接口契约
- Java：`FlowEngine.startProcess / executeNode / completeTask / rollback`；`NodeExecutor.execute(context)`；`ProcessInstanceService`。
- 事件：通过 Spring Event 发布 `PROCESS_STARTED/NODE_ENTERED/PROCESS_COMPLETED`（TRD §5.3，供 005/017 消费）。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 003 的解析器；userTask 节点内部创建任务（可先用简单 stub 断言任务被创建）
# 集成测试用一段 JSON 流程（start→userTask→end）发起并推进
```

### 验证用例（映射 PRD §运行引擎 验收标准 / TRD §4.1 / §5）
- [x] 发起实例：返回实例且 status=运行；创建耗时本地 < 500ms（记录基线）。
- [x] 顺序流转：start→userTask→end 跑通，最终 status=已完成。
- [x] 条件分支：`${amount>100}` 走不同分支正确（对应 PRD 支持条件分支流转）。
- [x] 并行网关：两分支均完成后汇聚再继续（对应 PRD 支持并行网关）。
- [x] 回退：可回退到指定节点。
- [x] 变量：发起传入变量，`${var}` 在条件与任务中可访问；节点级变量不污染全局。
- [x] 状态机：suspend→resume 恢复；terminate 后实例不可再操作（不可逆）。
- [x] 乐观锁：并发完成同一任务时后者失败（version 冲突）。

### 验证脚本/测试
- `FlowEngineE2ETest`：用内联 JSON 流程断言上述各路径。
- `ProcessVariableTest`：EL 取值/作用域。
- `StateMachineTest`：状态跃迁与终止不可逆。

## 交付物
- FlowEngine / NodeExecutor / ProcessInstanceService / 变量与状态机 / Caffeine 缓存配置。

## 验收门禁（DoD）
- 可发起并正常流转；条件/并行/回退正确；变量与状态机生效；并发安全（乐观锁）通过。
