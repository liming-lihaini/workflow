# ISSUE-011：节点插件体系 — 内置全量 + 自定义扩展

- **状态**：已完成
- **优先级**：P1
- **模块**：节点插件体系
- **负责人**：后端×2
- **预计工期**：W9
- **前置依赖**：ISSUE-002, ISSUE-004
- **里程碑**：M3

## 目标
补齐全部内置节点类型并验证插件扩展机制：实现 start/end/userTask/serviceTask/exclusiveGateway/parallelGateway/inclusiveGateway/scriptTask/subProcess，并提供一个自定义节点样例（onEnter/execute/onLeave 三阶段）。

## 范围
### In Scope
- 内置节点：`node/impl/*`（Superpowers 计划 §节点实现；TRD §4.1）。
  - start/end、userTask（接 005）、serviceTask（调用外部/脚本）、exclusiveGateway、parallelGateway（汇聚）、inclusiveGateway、scriptTask、subProcess（嵌套子流程）。
- 自定义扩展样例：`CustomDemoNodeHandler` 实现 onEnter/execute/onLeave，验证 `@Component` 自动注册即可被引擎识别（PRD §自定义节点扩展）。
- 节点配置 Schema：`getConfigSchema()` 返回可配置项（供 019 设计器渲染）。

### Out of Scope
- 可视化配置 UI（019）；会签/加签节点（006/007 已实现专属处理器）。

## 技术约束（以 TRD 为准）
- 节点即插件：实现 `NodeHandler` + `@Component` 即被 `NodeHandlerRegistry` 注册（002 框架）。
- 并行/子流程并发安全：本地锁 + 乐观锁（§5.2）。

## 接口契约
- Java：`NodeHandler`（已稳定，002）；新增内置类型常量；`NodeHandlerRegistry` 注册 discover。
- 扩展文档：插件开发规范（实现接口 + 注册）。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 002/004；用流程 JSON 引用各内置节点类型，断言各自行为
```

### 验证用例（映射 PRD §节点插件体系 验收标准 / TRD §1.1/§4）
- [x] start/end 正确开始/结束流程。
- [x] userTask 创建任务（与 005 衔接）。
- [x] serviceTask/scriptTask 执行无阻塞并传递变量。
- [x] exclusiveGateway 按条件选支；parallelGateway 汇聚；inclusiveGateway 多支。
- [x] subProcess 正确进入/退出嵌套流程。
- [x] 自定义节点样例被自动注册，`getHandler("customDemo")` 可用；三阶段（onEnter/execute/onLeave）均执行。

### 验证脚本/测试
- `BuiltinNodeTest`：各内置节点行为断言。
- `CustomNodeExtensionTest`：样例节点注册与三阶段执行。

## 交付物
- 全部内置节点实现 + 自定义节点样例 + 插件开发规范。

## 验收门禁（DoD）
- 内置节点类型齐全且行为正确；自定义节点通过实现接口即可被引擎加载执行。

## 测试结果
- 测试用例总数：19 个（BuiltinNodeTest 11 + CustomNodeExtensionTest 8）
- 全部 142 个测试通过（含历史测试）

## 交付物清单
- 内置节点处理器：start/end/serviceTask/scriptTask/exclusiveGateway/parallelGateway/inclusiveGateway/subProcess
- 自定义节点样例：CustomDemoNodeHandler（演示三阶段生命周期）
- NodeExecutor 支持全量节点类型流转
- 测试：BuiltinNodeTest + CustomNodeExtensionTest
