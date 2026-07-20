# ISSUE-001：项目脚手架与基础设施（SQLite 建表 / 启动 / 健康检查）

- **优先级**：P0
- **模块**：基础设施层
- **负责人**：架构师 + 后端×1
- **预计工期**：W1
- **前置依赖**：无
- **里程碑**：M1
- **状态**：✅ 已完成

## 目标
搭建可启动的 Spring Boot 工程，建立 SQLite 数据库与全部 19 张表（TRD §2.1），提供统一响应体与错误码（TRD §8），使后续每个 Issue 都能在“应用可启动 + 表已存在”的基础上独立开发。

## 范围
### In Scope
- `pom.xml`（Spring Boot 3.2 / JDK17 / MyBatis-Plus 3.5.x / SQLite JDBC / Caffeine）。
- 启动类 `FlowEngineApplication`、统一 `application.yml`（SQLite 数据源、Caffeine、sql-init）。
- `src/main/resources/db/schema.sql`：按 TRD §2.1 的 19 张表建表（流程定义/实例/任务/表单/数据模型/模型实例/变量/部门/用户/用户兼职/角色/用户角色/权限/角色权限/数据权限/访问日志/操作日志/字典类型/字典项）。
- 统一响应 `Result<T>`（TRD §8.1）与全局异常处理器（错误码 TRD §8.2）。
- 健康检查 `/actuator/health`（spring-boot-starter-actuator）。

### Out of Scope
- 具体业务接口实现（交由 002 及后续 Issue）。
- Redis/MySQL（TRD 明确使用 SQLite + Caffeine）。

## 技术约束（以 TRD 为准）
- 数据库：**SQLite 3.x**；ORM：**MyBatis-Plus 3.5.x**；缓存：**Caffeine**。
- 表名以 `wf_`（流程/表单/数据模型/变量）与 `sys_`（后台）前缀区分（TRD §2.1）。
- 统一响应：`{code,message,data,timestamp,requestId}`。

## 接口契约
- `GET /actuator/health` → 200，status=UP。
- 统一响应包装器 `Result<T>`（code/msg/data），全局异常映射错误码（200/400/401/403/404/500/1001~1005）。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 启动后自动执行 schema.sql 建表；访问 http://localhost:8080/actuator/health
```

### 验证用例（映射 TRD §2.1 / §8）
- [x] 应用启动无报错，日志显示 19 张表创建/已存在。
- [x] `GET /actuator/health` 返回 200 且 status=UP。
- [x] 访问任意未实现路径返回统一 `Result` 结构（code=404）。
- [x] 构造一个触发异常的请求，验证返回 TRD §8.2 错误码与 message。

### 验证脚本/测试
- 集成测试 `SchemaInitTest`：`@SpringBootTest` 注入 `JdbcTemplate`，断言 19 张表均存在（`SELECT count(*) FROM sqlite_master WHERE type='table'`）。
- 健康检查测试：`TestRestTemplate` 调用 `/actuator/health` 断言 200。

## 交付物
- 可启动工程、schema.sql（19 表）、Result/异常处理器、actuator 配置。

## 验收门禁（DoD）
- 工程可 `mvn spring-boot:run` 启动；19 张表存在；统一响应与错误码生效。

## 实际交付记录
- `pom.xml`：Spring Boot 3.2 + SQLite JDBC + Caffeine + Actuator + Lombok + MyBatis-Plus
- `application.yml`：SQLite 数据源 + sql-init 自动建表 + Caffeine 缓存 + Actuator 暴露 health/info + 404 兜底
- `db/schema.sql`：19 张建表脚本（wf_process_definition, wf_process_instance, wf_task, wf_form_definition, wf_data_model, wf_model_instance, wf_variable, sys_department, sys_user, sys_user_position, sys_role, sys_user_role, sys_permission, sys_role_permission, sys_data_permission, sys_access_log, sys_operation_log, sys_dict_type, sys_dict_item）
- `FlowEngineApplication.java`：Spring Boot 启动类
- `Result<T>` / `ErrorCode` / `BusinessException` / `RequestContext` / `RequestIdFilter` / `GlobalExceptionHandler`：统一响应与全局异常处理
- `PingController`：脚手架自测端点（/api/ping, /api/ping/fail, /api/ping/boom）
- 集成测试：`SchemaInitTest`（19 表断言 + 核心表可写）、`HealthCheckTest`（Actuator health + ping）、`CommonResponseTest`（业务异常/系统异常/404 兜底）
