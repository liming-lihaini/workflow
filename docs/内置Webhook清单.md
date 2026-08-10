# 内置 Webhook 清单

> 本文档汇总流程引擎内置的 Webhook 机制：触发事件、内置回调接口、内置/预置 Webhook 配置清单、管理 API 与载荷模板语法。
>
> **适用版本**：flow-engine（ISSUE-012 Webhook 回调） | **更新日期**：2026-08-10

---

## 1. Webhook 机制概述

流程引擎在流程/节点生命周期的关键节点发布 Spring 事件，由 `WebhookEventListener`（事务提交后 `AFTER_COMMIT` 触发）匹配 `wf_webhook` 表中启用状态的配置，交由 `WebhookScheduler` 异步发起 HTTP 回调，并将每次调用的请求/响应记录到 `wf_webhook_log`。

**核心组件**：

| 组件 | 路径 | 职责 |
|---|---|---|
| `WebhookEventListener` | `flow-engine/src/main/java/com/flow/engine/listener/WebhookEventListener.java` | 监听流程事件，匹配 Webhook 配置并异步调度 |
| `WebhookScheduler` | `flow-engine/src/main/java/com/flow/engine/service/WebhookScheduler.java` | 发起 HTTP 调用、模板渲染、失败重试 |
| `WebhookService` | `flow-engine/src/main/java/com/flow/engine/service/WebhookService.java` | Webhook 配置 CRUD 与按事件匹配 |
| `WebhookController` | `flow-engine/src/main/java/com/flow/engine/controllers/WebhookController.java` | 配置管理 API + 留样状态回调接口 |
| `HazardousWebhookController` | `flow-engine/src/main/java/com/flow/engine/controllers/HazardousWebhookController.java` | 危化品台账同步回调接口 |
| 数据表 | `wf_webhook` / `wf_webhook_log` | Webhook 配置 / 回调日志 |

---

## 2. 触发事件类型

| 事件类型 | 中文 | 触发时机 | 匹配维度 |
|---|---|---|---|
| `PROCESS_STARTED` | 流程启动 | 流程实例创建提交后 | processKey |
| `NODE_ENTERED` | 节点进入 | 进入节点提交后 | processKey + nodeId |
| `NODE_COMPLETED` | 节点完成 | 节点完成提交后 | processKey + nodeId |
| `PROCESS_COMPLETED` | 流程完成 | 流程实例结束提交后 | processKey |

> 另有两种非事件触发方式（仅出现在日志中）：
> - `CONNECTIVITY_TEST` —— 通过 `POST /api/v1/webhooks/{webhookKey}/test` 连通性测试触发
> - `MANUAL_TRIGGER` —— 通过 `POST /api/v1/webhooks/{webhookId}/trigger` 手动触发

**匹配规则**（`WebhookService.findWebhooksByEvent`）：
1. `status = 1`（启用）；
2. `process_key` 为空则匹配所有流程，否则必须相等；
3. `trigger_events` 为空则匹配所有事件，否则必须包含当前事件类型；
4. `node_id` 为空表示流程级 Webhook（任意节点都触发），否则必须等于当前节点 ID。

---

## 3. 内置回调接口（引擎提供的 Webhook 接收端）

以下接口由引擎自身暴露，可作为 Webhook 配置的 `url` 目标（自回调）。

### 3.1 修改留样状态

| 项 | 内容 |
|---|---|
| 接口 | `POST /api/v1/webhooks/retain-status` |
| 实现 | `WebhookController.updateRetainStatus` |
| 用途 | 按留样编号更新 `t_retain.status`（留样中 → 销毁审批中 → 已销毁） |
| 请求体 | `{ "retainNo": "留样编号", "status": "目标状态" }`，两字段均必填 |
| 响应 | `{ code: 0, data: { retainNo, status, updated } }`；缺参返回 `code=400` |
| 配套服务 | `EmsRetainService.updateStatusByRetainNo(retainNo, status)` |
| 典型使用 | 绑定 `LYXHSQ` 留样销毁流程，节点完成时携带流程变量 `formData.retainNo` 自动流转留样状态 |

### 3.2 危化品台账同步（入库叠加/新增）

| 项 | 内容 |
|---|---|
| 接口 | `POST /api/webhook/hazardous/upsert` |
| 实现 | `HazardousWebhookController.upsert` |
| 用途 | `WXPRKSQ` 危险品入库审批通过后同步 `t_hazardous_ledger`：相同 CAS 编号则库存叠加（MERGE），否则新增记录（INSERT，状态默认「在库」） |
| 请求体 | 完整事件 payload 即可（兼容 `formData` / `variables` / 顶层字段三种取法），必需字段：`casNo`、`qty`；可选：`name`、`category`、`unit`、`location` |
| 响应 | `{ code: 0, data: { casNo, inboundQty, action: MERGE/INSERT, ledgerId, newQty, ... } }`；缺 casNo/qty 返回 `code=400` |
| 典型使用 | 绑定 `WXPRKSQ` 流程技术审批节点（如 `userTask_4`）的 `NODE_COMPLETED` 事件 |

### 3.3 新设备入库台账登记（单台/批量）

| 项 | 内容 |
|---|---|
| 接口 | `POST /api/webhook/instrument/inbound` |
| 实现 | `InstrumentWebhookController.inbound` |
| 用途 | `SBTKRKSQ`（单台）/ `SBTKRKSQ_PL`（批量）新设备入库流程审批通过后写入 `t_instrument` 设备台账：单台读 `formData` 平铺字段；批量读 `formData.devices` 子表逐行入库 |
| 关键字段 | `create_by` = 流程申请人（`formData.applicant`）；`create_time` = 审批通过时间（回调触发时刻）；状态为空时默认「在用」 |
| 请求体 | 完整事件 payload 即可（兼容 `formData` / `variables` / 顶层字段三种取法），必需字段：`code`（仪器编号） |
| 幂等 | 相同仪器编号已存在时跳过（`skipped`），避免 Webhook 重试产生重复数据 |
| 响应 | `{ code: 0, data: { createBy, approveTime, inserted: [...], skipped: [...] } }`；无可入库数据返回 `code=400` |
| 典型使用 | 绑定 `SBTKRKSQ` / `SBTKRKSQ_PL` 审批节点（`userTask_approve`）的 `NODE_COMPLETED` 事件 |

---

## 4. 内置（代码注册）Webhook 配置清单

以下 Webhook 由代码在启动时自动注册/热更（幂等），无需手工创建：

| # | webhookKey | 名称 | 注册位置 | 触发事件 | 绑定流程 | 绑定节点 | 回调地址 |
|---|---|---|---|---|---|---|---|
| 1 | `retain_dispose_status` | 留样销毁-修改留样状态 | `DatabaseMigration.initRetainDisposeWebhook()` | `NODE_COMPLETED` | `LYXHSQ` | 无（流程级） | `http://localhost:8080/api/v1/webhooks/retain-status` |
| 2 | `instrument_inbound_single_hook` | 设备入库(单台)-台账登记 | `DatabaseMigration.initInstrumentInboundProcesses()` | `NODE_COMPLETED` | `SBTKRKSQ` | `userTask_approve` | `http://localhost:8080/api/webhook/instrument/inbound` |
| 3 | `instrument_inbound_batch_hook` | 设备入库(批量)-台账登记 | `DatabaseMigration.initInstrumentInboundProcesses()` | `NODE_COMPLETED` | `SBTKRKSQ_PL` | `userTask_approve` | `http://localhost:8080/api/webhook/instrument/inbound` |

**载荷模板**：

```json
{"retainNo":"${formData.retainNo}","status":"销毁审批中"}
```

- `${formData.retainNo}` 取自流程变量（`LYXHSQ` 流程启动时写入）；
- `status` 默认写「销毁审批中」，可在 Webhook 配置中改为其他状态值（如「已销毁」）；
- `node_id` 为空表示流程级触发，如需限定到具体审批节点，可在前端 Webhook 配置中填入该节点的实际 nodeId；
- 每次应用启动都会以代码中的定义覆盖更新该记录（可热更）。

**设备入库 Webhook（#2、#3）说明**：

- `payload_template` 为空，直接发送完整事件 payload（含 `formData`：申请人 `applicant`、单台平铺字段 / 批量 `devices` 子表数组）；
- 配套流程同样由 `DatabaseMigration.initInstrumentInboundProcesses()` 种子化（幂等热更）：
  - `SBTKRKSQ` 新设备入库申请(单台)：绑定表单 `instrument_inbound`，开始 -> 设备管理员审批(`userTask_approve`，处理人 `sys_admin`) -> 结束；
  - `SBTKRKSQ_PL` 新设备入库申请(批量)：绑定表单 `instrument_inbound_batch`（子表 `devices` 一次录入多台），节点结构同上；
- 审批节点通过即回调入库接口：创建人 = 申请人，创建时间 = 审批通过时间，重复编号自动跳过。

---

## 5. 预置数据中的其他 Webhook（随数据库初始化导入）

以下记录存在于 `wf_webhook` 初始数据中（非代码自动注册），供参考与清理：

| id | webhookKey | 名称 | 触发事件 | 绑定流程 | 绑定节点 | 回调地址 | 备注 |
|---|---|---|---|---|---|---|---|
| 1 | `WXPRKSQ_TECH_APPROVE_HAZARDOUS` | 入库审批-危化品台账同步 | `NODE_COMPLETED` | `WXPRKSQ` | `userTask_4` | `http://localhost:8080/api/webhook/hazardous/upsert` | 危险品入库审批通过后台账同步 |
| 2 | `update_hazardous` | 更新危化品信息 | `NODE_COMPLETED`、`PROCESS_COMPLETED` | （全部流程） | `userTask_4` | `http://localhost:8080/api/webhook/hazardous/upsert` | 未限定流程，任意流程的 userTask_4 完成都会触发 |
| 3 | `11` | 11 | `NODE_ENTERED` | （全部流程） | `userTask_4` | （url 为空） | 测试残留数据，建议删除 |
| 4 | `retain_dispose_status` | 留样销毁-修改留样状态 | `NODE_COMPLETED` | `LYXHSQ` | 无 | `http://localhost:8080/api/v1/webhooks/retain-status` | 代码内置（见第 4 节） |
| 5 | `modify-retain-status` | modify-retain-status | `NODE_COMPLETED` | `LYXHSQ` | `userTask_1` | `http://localhost:8080/api/v1/webhooks/retain-status` | 手工创建，限定留样销毁流程 userTask_1 节点 |

> 注：回调地址使用 `http://localhost:8080` 为开发环境自回调；部署到其他环境时需改为对应的外部可访问地址。

---

## 6. Webhook 管理 API

基础路径：`/api/v1/webhooks`（`WebhookController`）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/webhooks` | 创建 Webhook 配置（`webhookKey` 全局唯一） |
| GET | `/api/v1/webhooks` | 列表查询，可选参数 `processKey` |
| GET | `/api/v1/webhooks/{webhookKey}` | 按 key 查询配置详情 |
| PUT | `/api/v1/webhooks/{webhookKey}` | 更新配置 |
| DELETE | `/api/v1/webhooks/{webhookKey}` | 删除配置 |
| POST | `/api/v1/webhooks/{webhookKey}/test` | 连通性测试（不执行业务，仅探测 URL 可达性，事件类型记为 `CONNECTIVITY_TEST`） |
| POST | `/api/v1/webhooks/{webhookId}/trigger` | 手动触发一次回调，可自定义 payload（事件类型记为 `MANUAL_TRIGGER`） |
| GET | `/api/v1/webhooks/logs` | 回调日志查询，可选参数 `webhookKey` / `processInstanceId` / `eventType` |
| POST | `/api/v1/webhooks/logs/{logId}/retry` | 对失败日志手动重试 |

**配置字段默认值**：`method=POST`、`timeout=5000`（毫秒）、`retryCount=3`、`status=1`（启用）。

---

## 7. 事件载荷（payload）结构

### 7.1 流程级事件（PROCESS_STARTED / PROCESS_COMPLETED）

```json
{
  "processInstanceId": 123,
  "processKey": "LYXHSQ",
  "startUser": "zhangsan",
  "timestamp": 1786000000000
}
```

> `startUser` 仅 `PROCESS_STARTED` 携带。

### 7.2 节点级事件（NODE_ENTERED / NODE_COMPLETED）

```json
{
  "processInstanceId": 123,
  "nodeId": "userTask_4",
  "nodeType": "userTask",
  "nodeName": "技术部门审批",
  "timestamp": 1786000000000,
  "variables": { "casNo": "7732-18-5", "qty": 10, "...": "..." },
  "formData": { "casNo": "7732-18-5", "qty": 10, "...": "..." }
}
```

> - `variables` / `formData` 为同一份流程变量（含表单字段），`NODE_COMPLETED` 时携带，便于下游接口直接读取表单字段；
> - 未配置 `payloadTemplate` 时，以上完整 JSON 即为请求体。

---

## 8. 载荷模板语法（payloadTemplate）

- 语法：`${path}`，支持嵌套路径 `${a.b.c}`（正则 `\$\{([\w.]+)\}`）；
- 取值按点号逐层从 payload Map 中解析，取不到时替换为空字符串；
- 未配置模板时，直接发送第 7 节的完整 payload JSON。

示例（留样状态回调）：

```json
{"retainNo":"${formData.retainNo}","status":"销毁审批中"}
```

---

## 9. 执行与重试机制

| 项 | 规则 |
|---|---|
| 触发时机 | 事务提交后（`@TransactionalEventListener(AFTER_COMMIT)`），异步调度，不阻塞流程主事务 |
| 超时 | 按配置的 `timeout`（默认 5000ms），超时视为失败并进入重试 |
| 重试策略 | 连接超时或目标返回 5xx 视为瞬时错误，自动重试至 `retryCount` 上限；4xx 为业务错误不重试 |
| 日志状态 | `wf_webhook_log.status`：0-失败、1-成功、2-重试中 |
| 手动重试 | `POST /api/v1/webhooks/logs/{logId}/retry` |

---

## 10. 前端配置入口

- 流程设计器（`flow-designer/src/components/ConfigPanel.jsx`）：节点配置面板的 **Webhook** 标签页，支持新建/编辑/删除/连通性测试，按节点 ID 过滤该节点的 Webhook 列表；
- 事件选项：流程启动、节点进入、节点完成、流程完成（对应第 2 节四个事件类型）。

---

## 相关文档

- `docs/issues/ISSUE-012-Webhook回调.md` —— Webhook 回调需求定义
- `webhook.md`（仓库根目录）—— 留样销毁 Webhook 验证记录
