# ISSUE-006：会签 — 四种模式

- **优先级**：P0
- **模块**：任务中心 / 会签
- **负责人**：后端×2
- **预计工期**：W7
- **前置依赖**：ISSUE-004, ISSUE-005
- **里程碑**：M3

## 目标
实现会签节点：多个审批人并行收到任务，按**全通过 / 按比例 / 一票通过 / 一票否决** 四种模式汇总结果，结果驱动流程继续或驳回。

## 范围
### In Scope
- 会签组与投票：`flow_counter_sign_group` / `flow_counter_sign_vote`（Superpowers 计划 §2.4/2.5；字段对齐 TRD 任务类型=2）。
- 服务：`CounterSignService.initCounterSign / submitVote / calculateResult / isComplete`（TRD §4.2.1）。
- 节点 `CounterSignNodeHandler`：进入时初始化会签，按模式等待投票。
- 投票 API：`POST /api/v1/tasks/{id}/counter-sign/vote`（TRD §3.3）。
- 四种模式结果算法（全通过=无拒绝；按比例=通过比例≥阈值；一票通过=有任一通过；一票否决=有任一拒绝即否）（PRD §会签模式）。

### Out of Scope
- 普通任务（005）；加签（007）。

## 技术约束（以 TRD 为准）
- 算法见 TRD §4.2.1 `calculateResult`；并发安全用乐观锁（§5.2）。
- 会签节点需在流程定义中配置多审批人（PRD §约束）。

## 接口契约
- REST：`POST /api/v1/tasks/{id}/counter-sign/vote`。
- Java：`CounterSignService`、`CounterSignNodeHandler`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 004/005；用一段含 counterSign 节点的流程 JSON 发起，分别验证四种模式
```

### 验证用例（映射 PRD §任务中心 会签验收 / TRD §4.2.1）
- [ ] 全通过：所有投票同意 → 会签通过，流程继续。
- [ ] 按比例：通过比例 ≥ 配置阈值 → 通过；否则驳回。
- [ ] 一票通过：任一同意即整体通过。
- [ ] 一票否决：任一拒绝即整体驳回。
- [ ] 多人并发投票，汇总计数准确（乐观锁无丢失更新）。
- [ ] 会签未完成时流程不推进；完成后按结果流转。

### 验证脚本/测试
- `CounterSignServiceTest`：四模式分别计算结果用例。
- `CounterSignE2ETest`：MockMvc 模拟多人投票 + 引擎推进。

## 交付物
- 会签实体/服务/节点处理器/投票接口 + 结果计算。

## 验收门禁（DoD）
- 四种模式均可正确汇总并驱动流转；并发投票无计数错误。
