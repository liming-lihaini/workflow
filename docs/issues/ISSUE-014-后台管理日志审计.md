# ISSUE-014：后台管理 — 访问/操作日志审计

- **优先级**：P0
- **状态**：✅ 已完成（2026-07-15）
- **模块**：后台管理 / 审计
- **负责人**：后端×2
- **预计工期**：W11
- **前置依赖**：ISSUE-013
- **里程碑**：M4

## 目标
记录访问日志与操作日志（含操作前后数据），支持查询与导出，为三员审计（016）与安全合规（TRD §6.3）提供数据。

## 范围
### In Scope
- 日志表：`sys_access_log`（§2.1.16）、`sys_operation_log`（§2.1.17）。
- 服务：`LogService.recordAccessLog/recordOperationLog/query*`（§5.5.4）。
- 拦截：通过 Spring 拦截器/注解自动记录访问与操作日志（TRD §6.3 操作审计）。
- API：`/api/v1/system/logs/access[|/export]`、`/operation[|/export]`（TRD §3.11）。
- 日志保留周期可配置（PRD §日志管理）。

### Out of Scope
- 三员审计视图（016）；脱敏展示细节（在 016 一并处理）。

## 技术约束（以 TRD 为准）
- 表 `sys_access_log`/`sys_operation_log`（§2.1.16/17）。
- API §3.11；审计字段（操作人/时间/类型/前后数据）§6.3。

## 接口契约
- REST：`/api/v1/system/logs/access`、`/access/export`、`/operation`、`/operation/export`。
- Java：`LogService`、`AccessLogInterceptor`、`OperationLogAspect`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 依赖 013 的用户/权限；发起若干请求后查询日志
```

### 验证用例（映射 PRD §后台管理 日志验收 / TRD §6.3）
- [x] 访问任意接口后，`sys_access_log` 出现对应记录（url/method/result/ip）。
- [x] 执行写操作（如更新用户）后，`sys_operation_log` 记录前后数据。
- [x] 查询接口可按用户/时间/模块过滤；导出返回文件。
- [ ] 敏感字段（手机号等）在日志中按脱敏策略处理（TRD §6.2）。（在 ISSUE-016 一并处理）

### 验证脚本/测试
- `LogServiceTest`：记录/查询/导出。
- `OperationLogAspectTest`：写操作自动落库且含前后数据。

## 交付物
- 日志服务 + 访问拦截器 + 操作切面 + 查询/导出接口。

## 验收门禁（DoD）
- [x] 访问/操作日志自动记录且可查可导；含前后数据；敏感字段脱敏（在 ISSUE-016 处理）。

## 实现摘要
- 2 个实体：AccessLog、OperationLog
- 2 个 Mapper：AccessLogMapper、OperationLogMapper
- 1 个核心服务：LogService（记录/查询/导出/清理/统计）
- 1 个访问日志拦截器：AccessLogInterceptor（自动记录所有 API 请求）
- 1 个操作日志切面：OperationLogAspect（配合 @OpLog 注解记录操作日志含前后数据）
- 1 个 REST Controller：LogController（`/api/v1/system/logs/*`）
- 1 个 WebMvc 配置：WebMvcConfig（注册拦截器）
- 1 个注解：@OpLog（标注在方法上自动记录操作日志）

## 测试覆盖
- LogServiceTest（9 用例）、OperationLogAspectTest（4 用例）
- 全部 167 个测试通过，BUILD SUCCESS
