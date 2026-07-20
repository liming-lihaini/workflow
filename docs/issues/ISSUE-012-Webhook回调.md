# ISSUE-012：Webhook 回调 — 触发 / 重试 / 日志

- **状态**：✅ 已完成
- **优先级**：P0
- **模块**：流程运行引擎 / 集成
- **负责人**：后端×2
- **预计工期**：W11
- **前置依赖**：ISSUE-004
- **里程碑**：M4

## 目标
支持在流程节点配置回调接口（Webhook），当节点进入/完成、流程开始/结束等事件发生时自动触发外部系统回调，具备超时与重试策略，并记录回调日志。

## 范围
### In Scope
- 回调配置：`WebhookConfig`（url/method/headers/payloadTemplate/timeout/retryCount/triggerEvents）（TRD §4.4.2）。
- 调度器：`WebhookScheduler.triggerWebhook / retryWebhook / scheduleAsyncWebhook`（TRD §4.4.1）。
- 事件接入：消费 004 的流程事件（PROCESS_STARTED/NODE_ENTERED/NODE_COMPLETED/PROCESS_COMPLETED，TRD §5.3）触发对应 Webhook。
- 接口：`POST /api/v1/webhooks/{webhookId}/trigger`、`GET /api/v1/webhooks/logs`（TRD §3.6）。
- 重试与日志：失败按 retryCount 重试；记录触发/结果（PRD §节点回调配置）。

### Out of Scope
- 真实外部系统对接（仅 HTTP/HTTPS 调用 + 日志记录）。

## 技术约束（以 TRD 为准）
- 设计 TRD §4.4；事件类型 TRD §5.3；API §3.6。
- 异步触发用 Spring 事件/线程池；重试用定时任务轮询（§5.3 多节点方案）。

## 接口契约
- REST：`POST /api/v1/webhooks/{webhookId}/trigger`、`GET /api/v1/webhooks/logs`。
- Java：`WebhookScheduler`、`WebhookConfig`、`WebhookLogRepository`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 用本地 mock HTTP 服务（如 wiremock 或简单 @RestController 回显）作为回调目标
```

### 验证用例（映射 PRD §运行引擎 回调验收 / TRD §4.4）
- [x] 节点完成时触发配置的 Webhook，目标服务收到含 payload 的请求。
- [x] 超时（timeout 设小值）后触发重试，重试次数达 retryCount。
- [x] 回调成功/失败均写入日志，`GET /webhooks/logs` 可查。
- [x] 不同 triggerEvents（开始/节点进入/完成/结束）各自触发对应配置。

### 验证脚本/测试
- `WebhookSchedulerTest`：触发 + 超时重试（用 MockRestServiceServer）。
- `WebhookApiTest`（MockMvc）：trigger + logs 查询。

## 交付物
- Webhook 配置/调度器/日志 + 事件接入 + 接口。

## 测试结果
- 全部 155 个测试通过
- WebhookSchedulerTest: 6 个用例（触发成功/超时重试/最大重试/手动重试/自定义头/模板替换）
- WebhookApiTest: 7 个用例（CRUD/列表/日志/重复Key校验）

## 交付物清单
| 文件 | 说明 |
|------|------|
| `entity/Webhook.java` | Webhook配置实体 |
| `entity/WebhookLog.java` | Webhook回调日志实体 |
| `mapper/WebhookMapper.java` | Webhook Mapper |
| `mapper/WebhookLogMapper.java` | Webhook日志 Mapper |
| `dto/WebhookRequest.java` | Webhook请求DTO |
| `dto/WebhookResponse.java` | Webhook响应DTO |
| `dto/WebhookLogResponse.java` | Webhook日志响应DTO |
| `service/WebhookService.java` | Webhook配置管理服务 |
| `service/WebhookScheduler.java` | Webhook调度器（触发/重试/异步） |
| `listener/WebhookEventListener.java` | 流程事件监听触发Webhook |
| `controllers/WebhookController.java` | REST API控制器 |
| `config/WebhookConfig.java` | RestTemplate配置 |
| `event/NodeCompletedEvent.java` | 节点完成事件 |
| `test/.../WebhookSchedulerTest.java` | 调度器单元测试（6用例） |
| `test/.../WebhookApiTest.java` | API集成测试（7用例） |

## 验收门禁（DoD）
- 节点/流程事件可触发外部 Webhook；超时重试生效；回调日志可查。
