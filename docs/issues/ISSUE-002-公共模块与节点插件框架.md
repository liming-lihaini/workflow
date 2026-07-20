# ISSUE-002：公共模块与节点插件框架

- **优先级**：P0
- **模块**：基础设施层 / 引擎核心
- **负责人**：后端×1
- **预计工期**：W2-W3
- **前置依赖**：ISSUE-001
- **里程碑**：M1
- **状态**：✅ 已完成

## 目标
建立公共基础能力（枚举/异常/Result/工具/EL 表达式）与**插件式节点框架**（NodeHandler/Registry/AutoConfig），为运行引擎（004）、任务（005）、插件体系（011）提供可扩展的执行骨架。节点处理器可在 Spring 容器中被自动注册与发现。

## 范围
### In Scope
- 公共模块：`common/enums`（ProcessStatus、TaskStatus、SignType、CounterSignMode…）、`common/exception`（FlowException…）、`common/utils`（JsonUtils、ExpressionUtils —— EL 表达式求值）。
- 节点框架：`NodeHandler` 接口（getNodeType / onEnter / execute / onLeave / getConfigSchema）、`AbstractNodeHandler`、`NodeHandlerRegistry`（ConcurrentHashMap 注册与按类型查找）、`NodeHandlerAutoConfiguration`（@PostConstruct 自动注册所有 NodeHandler Bean）。
- `ExecutionContext`（流程/节点/变量/操作人，PRD §运行引擎 变量与 TRD §4.1.2）。

### Out of Scope
- 具体内置节点实现（start/end/userTask… 放 ISSUE-011）。
- 引擎驱动逻辑（放 ISSUE-004）。

## 技术约束（以 TRD 为准）
- 节点即插件：通过实现 `NodeHandler` 并注册到 `NodeHandlerRegistry` 扩展（TRD §1.1 应用层 / §4 节点回调）。
- 并发安全：注册表使用 `ConcurrentHashMap`（TRD §5.2 本地锁+乐观锁场景）。
- EL 表达式：支持 `${var}` 形式访问流程/节点变量（PRD §流程变量）。

## 接口契约
- Java：`NodeHandler`（接口签名稳定，供 004/011 调用）、`NodeHandlerRegistry.getHandler(nodeType)`、`ExecutionContext.getVariable/setVariable`。
- 扩展点：新增节点只需实现 `NodeHandler` 并声明 `@Component`，无需改动框架。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run   # 启动后 NodeHandlerAutoConfiguration 自动注册已声明的节点
```
> 本 Issue 可暂不声明任何业务节点，仅注册框架；用测试用 Dummy 节点验证注册/发现链路。

### 验证用例（映射 PRD §节点插件体系 / TRD §4）
- [x] 单元测试：自定义一个 `DummyNodeHandler`，Spring 启动后 `registry.getHandler("dummy")` 能返回该实例。
- [x] `registry.getHandler("not-exist")` 抛出 FlowException（节点类型不存在）。
- [x] 单元测试：ExpressionUtils 对 `${amount > 100}` 在 variables={amount:200} 下求值为 true；`${name}` 取值正确。
- [x] `ExecutionContext.setVariable/getVariable` 在全局与本地作用域行为正确（PRD 变量作用域）。

### 验证脚本/测试
- `NodeHandlerRegistryTest`：断言注册、查找、不存在抛异常。
- `ExpressionUtilsTest`：覆盖比较/逻辑/变量取值表达式。
- `ExecutionContextTest`：全局/本地变量覆盖优先级。

## 交付物
- common 模块全套、节点插件框架（NodeHandler/Registry/AutoConfig/ExecutionContext）。

## 验收门禁（DoD）
- 节点可自动注册与按类型发现；EL 表达式求值正确；公共枚举/异常/工具可用。
