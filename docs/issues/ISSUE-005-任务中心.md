# ISSUE-005：任务中心 — 待办/已办/签收/转办/委派/通过/驳回

- **优先级**：P0
- **模块**：任务中心
- **状态**：✅ 已完成
- **负责人**：后端×2
- **预计工期**：W6
- **前置依赖**：ISSUE-004
- **里程碑**：M2

## 目标
提供任务的全生命周期管理：userTask 节点执行时生成任务，支持待办/已办查询、签收/取消签收、通过/驳回、转办/委派，并消费 004 的 `NODE_ENTERED` 事件自动建任务。

## 范围
### In Scope
- 任务 CRUD 与查询：`/api/v1/tasks/todo`、`/done`、`/{id}`（TRD §3.3）。
- 操作：`/{id}/claim`、`/unclaim`、`/complete`、`/reject`、`/transfer`、`/delegate`（TRD §3.3）。
- 任务生成：userTask 节点进入时自动创建任务（消费 NODE_ENTERED，assignee 来自节点候选人）。
- 约束：签收后仅签收人或管理员可处理（PRD §任务管理）。
- 状态映射：`wf_task.status`（0-待处理/1-处理中/2-已完成，TRD §2.1.3）。

### Out of Scope
- 会签/加签（006/007）；表单数据绑定（008）；表单权限（009）。

## 技术约束（以 TRD 为准）
- 表：`wf_task`（TRD §2.1.3）、`task:todo:{userId}` Caffeine 缓存（§5.4）。
- API `/api/v1/tasks/*`（TRD §3.3）。
- 任务完成回调引擎推进节点（与 004 `completeTask` 衔接）。

## 接口契约
- REST：`/api/v1/tasks/todo|done|{id}|{id}/claim|unclaim|complete|reject|transfer|delegate`。
- Java：`TaskService`：create/claim/unclaim/complete/reject/transfer/delegate/getTodo/getDone；`UserTaskNodeHandler` 调用 `TaskService.createTask`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 004 引擎（userTask 节点会创建任务）；可单独用 TaskService 单测建/查/操作任务
```

### 验证用例（映射 PRD §任务中心 验收标准 / TRD §3.3）
- [x] 流程流转到 userTask，待办列表出现该任务；完成后移入已办。
- [x] 签收：签收后 `assignee` 更新；非签收人 `complete` 被拒（403）。
- [x] 取消签收：任务回到候选。
- [x] 通过：任务完成并触发引擎推进到下一节点。
- [x] 驳回：实例回到上一节点/发起人（按设计）。
- [x] 转办：任务处理人变更；委派：原处理人保留，受托人处理。

### 验证脚本/测试
- `TaskApiTest`（MockMvc）：todo/done/claim/complete/reject/transfer/delegate 全链路。
- `UserTaskCreationTest`：发起流程断言 userTask 自动建任务。

## 交付物
- TaskService + TaskController + UserTaskNodeHandler 建任务逻辑 + 缓存。

## 验收门禁（DoD）
- 待办/已办查询正确；签收/转办/委派/通过/驳回均生效，且权限约束（签收人 exclusive）通过。
