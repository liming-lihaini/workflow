# ISSUE-017：流程监控 — 轨迹 / 变量 / 耗时 / 干预 / 导出

- **优先级**：P2
- **模块**：流程监控
- **负责人**：后端×1 + 前端×2
- **预计工期**：W12
- **前置依赖**：ISSUE-004
- **里程碑**：M4
- **状态**：✅ 已完成

## 目标
提供流程实例运行时监控：查看执行轨迹、变量历史、耗时统计，并支持管理员手动干预（流转/异常恢复）与数据导出。

## 范围
### In Scope
- 监控数据来源：节点执行记录 `flow_node_execution`（Superpowers §2.10）、变量 `wf_variable`。
- 服务：`ProcessMonitorService.getExecutionHistory/getStatistic/getRunningProcesses`（Superpowers §Task13；对齐 TRD 监控能力）。
- 手动干预：管理员强制推进/跳转到节点、异常恢复；操作记日志（PRD §流程管理）。
- 轨迹/变量/耗时查询接口（扩展 TRD §3.2 实例详情）。
- 导出：实例数据/轨迹导出。

### Out of Scope
- 实时大盘 UI（由 018 前端承载）；性能采集（M5 压测）。

## 技术约束（以 TRD 为准）
- 数据来自节点执行记录与变量表；手动干预需记录操作日志（§6.3，依赖 014）。
- 高可用/集群下多节点通过定时任务轮询（§5.3）。

## 接口契约
- REST：`GET /api/v1/monitor/instances/{id}/history`、`/statistics`、`/running`；干预 `POST /instances/{id}/intervene`；导出 `GET /instances/{id}/export`。
- Java：`ProcessMonitorService`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 004；先发起并流转一条流程，再查询监控数据
```

### 验证用例（映射 PRD §流程监控 验收标准 / TRD 监控能力）
- [x] 查询执行轨迹：按进入/离开时间顺序展示节点（含 start→…→end）。
- [x] 变量历史：展示流程/节点级变量及变更。
- [x] 耗时统计：实例总耗时与各节点耗时正确。
- [x] 手动干预：管理员强制推进到指定节点，记录操作日志（与 014 衔接）。
- [x] 导出：返回轨迹/数据文件。

### 验证脚本/测试
- `ProcessMonitorServiceTest`：11 个测试用例，覆盖轨迹/变量/耗时聚合、干预操作。

## 交付物
- 监控服务 + 轨迹/变量/耗时接口 + 手动干预 + 导出。

## 验收门禁（DoD）
- 可查看执行轨迹/变量历史/耗时；管理员可干预并留痕；支持导出。

## 实现摘要

### 新增文件
| 文件 | 说明 |
|------|------|
| `ProcessMonitorService.java` | 流程监控服务：执行轨迹、变量历史、耗时统计、运行中流程、导出、干预 |
| `MonitorController.java` | REST API：/api/v1/monitor/* |
| `ProcessMonitorServiceTest.java` | 11 个测试用例 |

### API 端点
| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/monitor/instances/{id}/history | GET | 获取执行轨迹 |
| /api/v1/monitor/instances/{id}/variables | GET | 获取变量历史 |
| /api/v1/monitor/instances/{id}/statistics | GET | 获取耗时统计 |
| /api/v1/monitor/running | GET | 获取运行中的流程列表 |
| /api/v1/monitor/instances/{id}/export | GET | 导出实例数据 |
| /api/v1/monitor/instances/{id}/intervene | POST | 管理员干预 |

### 测试覆盖
- 全部 227 个测试通过，其中 ProcessMonitorServiceTest 11 个用例：
  - 获取执行轨迹
  - 获取变量历史
  - 获取耗时统计
  - 获取运行中的流程列表
  - 导出实例数据
  - 管理员干预-成功
  - 管理员干预-实例不存在
  - 管理员干预-实例不在运行状态
  - 查询不存在的实例-执行轨迹
  - 查询不存在的实例-变量历史
  - 查询不存在的实例-耗时统计
