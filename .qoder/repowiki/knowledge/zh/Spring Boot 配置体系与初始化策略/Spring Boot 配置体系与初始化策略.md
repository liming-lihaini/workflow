---
kind: configuration_system
name: Spring Boot 配置体系与初始化策略
category: configuration_system
scope:
    - '**'
source_files:
    - flow-engine/src/main/resources/application.yml
    - flow-engine/src/test/resources/application-test.yml
    - flow-engine/src/main/java/com/flow/engine/config/CacheConfig.java
    - flow-engine/src/main/java/com/flow/engine/config/MybatisPlusConfig.java
    - flow-engine/src/main/java/com/flow/engine/config/WebMvcConfig.java
    - flow-engine/src/main/java/com/flow/engine/config/WebhookConfig.java
    - flow-engine/src/main/java/com/flow/engine/config/DictDataInitializer.java
    - flow-engine/src/main/java/com/flow/engine/config/TripleAdminInitializer.java
---

## 配置系统概述

该项目采用 Spring Boot 3.2.0 作为后端框架，使用标准的 YAML 配置文件和 Java Config 类进行应用配置管理。配置系统遵循 Spring Boot 约定优于配置的原则，通过分层配置实现开发、测试和生产环境的差异化部署。

## 核心配置文件

### 主配置文件 (application.yml)
- **服务器配置**: 默认端口 8080，禁用静态资源映射
- **数据库配置**: SQLite 嵌入式数据库，连接字符串 `jdbc:sqlite:flow_engine.db`
- **数据初始化**: 启动时自动执行 `db/schema.sql` 建表脚本
- **缓存配置**: Caffeine 本地缓存，最大容量 10000，过期时间 1 小时
- **监控端点**: 暴露 health 和 info 健康检查接口

### 测试配置文件 (application-test.yml)
- **内存数据库**: 使用 `jdbc:sqlite::memory:` 确保测试隔离性
- **动态端口**: 服务器端口设为 0，避免端口冲突
- **SQL 分隔符**: 显式设置 `;` 作为 SQL 语句分隔符

## Java 配置类架构

### 基础设施配置
- **CacheConfig**: 配置 Caffeine 缓存管理器，设置最大容量和过期策略
- **MybatisPlusConfig**: 注册分页插件和乐观锁插件，支持 SQLite 方言
- **WebMvcConfig**: 配置访问日志拦截器，排除认证和日志查询接口
- **WebhookConfig**: 配置专用 RestTemplate，设置 5 秒超时限制

### 数据初始化策略
项目采用 `CommandLineRunner` 实现启动时数据初始化：
- **DictDataInitializer**: 初始化系统内置字典数据（流程状态、节点类型等）
- **TripleAdminInitializer**: 初始化三员管理员账号和角色
- 通过 `@Order` 注解控制初始化顺序

## 配置加载机制

### 环境变量支持
虽然未发现显式的 `@Value` 或 `Environment` 使用，但 Spring Boot 原生支持通过环境变量覆盖配置文件属性，例如：
- `SPRING_DATASOURCE_URL` 可覆盖数据库连接
- `SERVER_PORT` 可覆盖服务端口

### 前端配置
Vue3 前端使用 Vite 的环境变量系统：
- `import.meta.env.DEV` 用于开发环境判断
- 通过 `.env` 文件管理前端配置

## 配置最佳实践

### 环境隔离
- 开发环境：SQLite 文件数据库，便于本地调试
- 测试环境：内存数据库，保证测试独立性和快速执行
- 生产环境：应切换为 MySQL/PostgreSQL 等关系型数据库

### 安全考虑
- 敏感配置（如数据库密码）应通过环境变量注入
- 当前配置文件中的用户名密码为空，依赖运行时注入
- Webhook 调用设置了合理的超时限制，防止阻塞

### 扩展性设计
- 配置类采用独立的 `@Configuration` 类组织，便于维护和替换
- 初始化逻辑通过 `CommandLineRunner` 接口实现，支持多阶段初始化
- 拦截器和配置类解耦，支持灵活的路径匹配和排除规则

## 配置演进建议

1. **引入配置中心**: 对于微服务架构，建议使用 Nacos 或 Apollo 管理配置
2. **加密敏感信息**: 使用 Jasypt 或云服务商的密钥管理服务
3. **配置验证**: 添加 `@Validated` 和自定义校验器确保配置有效性
4. **配置文档化**: 使用 Spring Boot Configuration Processor 自动生成配置元数据