# Workflow — 环境监测 LIMS 工作流平台

一个面向**环境监测 LIMS（实验室信息管理系统）**的流程驱动业务平台，包含可拖拽的流程设计器、自研工作流引擎以及基于 Vue 3 的业务前端。平台通过工作流引擎统一编排采样、检测、报告、留样销毁、合同、仪器、危废等环境监测核心业务，并提供 Webhook 机制将流程事件与业务系统打通。

## 项目简介

本项目由三大模块组成，采用前后端分离架构：

| 模块 | 技术栈 | 说明 |
| --- | --- | --- |
| `flow-engine` | Java 17 / Spring Boot 3.2 / MyBatis-Plus / MySQL 8 | 自研工作流引擎与业务后端，提供流程编排、任务流转、Webhook、权限、基础数据等能力 |
| `flow-web` | Vue 3 + Vite + Ant Design Vue + vxe-table | 业务前端（采样、检测、报告、留样销毁、系统管理等） |
| `flow-designer` | React 18 + Vite + React Flow | 可视化流程设计器（独立部署，供前端嵌入使用） |

## 目录结构

```
workflow/
├── flow-engine/        # 后端工作流引擎 + 业务接口（Spring Boot）
│   ├── src/main/java/com/flow/engine/
│   │   ├── controllers/    # 对外 REST 接口（流程/任务/权限/采样/检测/报告/Webhook…）
│   │   ├── service/        # 业务逻辑层
│   │   ├── mapper/         # MyBatis-Plus 数据访问层
│   │   ├── entity/         # 数据库实体
│   │   ├── engine/         # 流程引擎核心（解析/节点/事件/监听）
│   │   ├── node/           # 流程节点定义与执行
│   │   ├── webhook/        # Webhook 触发与回调
│   │   ├── config/         # 框架与缓存配置（Caffeine）
│   │   └── listener/       # 事件监听器
│   ├── src/main/resources/
│   │   ├── application.yml  # 数据源 / 缓存 / 上传 / 端口配置
│   │   └── db/schema_mysql.sql  # 建表脚本（启动时自动执行）
│   └── pom.xml
├── flow-web/           # Vue 3 业务前端
│   ├── src/views/      # 页面：dashboard / ems / process / task / system / login …
│   ├── src/api/        # 接口封装（axios）
│   ├── src/components/ # 公共组件
│   ├── src/stores/     # Pinia 状态管理
│   └── vite.config.js  # 开发代理 /api -> :8080
├── flow-designer/      # React 流程设计器
│   ├── src/App.jsx     # 设计器主界面（基于 React Flow）
│   ├── src/nodeTypes.js# 自定义节点类型
│   └── vite.config.js  # 开发代理 /api -> :8080
└── docs/               # 产品/技术文档（PRD / BRD / TRD / 测试报告 / 权限清单 …）
```

## 技术栈

**后端 `flow-engine`**
- Spring Boot 3.2.0，Java 17
- MyBatis-Plus 3.5.5 数据访问
- MySQL 8.0（Connector/J 8.3.0），启动时自动建表
- Caffeine 本地缓存（最大 10000 条，写后 1h 过期）
- Spring AOP + 缓存 + 调度（`@EnableCaching` / `@EnableScheduling`）
- Groovy 3.0 脚本支持（用于动态脚本节点）
- EasyExcel 3.3.4 导入导出
- Actuator 健康监控（`/actuator/health`）

**前端**
- `flow-web`：Vue 3.5 + Vite 5 + Ant Design Vue 4 + vxe-table 3 + Pinia + Vue Router + @vue-flow/core
- `flow-designer`：React 18 + Vite 5 + React Flow 11

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+（建议 20）
- MySQL 8.0（默认库名 `flow_engine`，账号 `root` / `123456`，可在 `application.yml` 修改）

## 快速开始

### 1. 后端 `flow-engine`

```bash
cd flow-engine

# 创建数据库（如未自动创建）
mysql -uroot -p123456 -e "CREATE DATABASE flow_engine DEFAULT CHARSET utf8mb4;"

# 编译打包
mvn clean package -DskipTests

# 运行（默认端口 8080，启动会自动执行 db/schema_mysql.sql 建表）
java -jar target/flow-engine-1.0.0.jar
```

> 配置见 `src/main/resources/application.yml`：数据源、上传限制（单文件 20MB）、Caffeine 缓存规格、Actuator 暴露端点。

### 2. 设计器 `flow-designer`

```bash
cd flow-designer
npm install
npm run dev      # 开发服务器 http://localhost:3001（/api 代理到 :8080）
npm run build    # 产物输出 dist/
npm run lint     # ESLint 检查
```

### 3. 业务前端 `flow-web`

```bash
cd flow-web
npm install
npm run dev      # 开发服务器 http://localhost:3000（/api 代理到 :8080）
npm run build    # 产物输出 dist/
npm run preview  # 预览构建产物
```

> 三个模块均通过 `/api` 反向代理对接后端 `:8080`，生产部署时请替换为实际网关地址。

## 核心能力

- **流程编排**：可视化设计器定义流程，引擎完成解析、节点执行与任务流转。
- **任务与待办**：流程任务生成、分配、审批、转办等。
- **Webhook 集成**：在流程节点完成时触发外部回调（如留样销毁、资产报废、危废处置等），支持 HTTP 服务任务与 `payloadTemplate` 模板变量。内置 Webhook 清单见 `docs/内置Webhook清单.md`。
- **权限体系**：用户 / 角色 / 部门 / 字典 / 菜单权限管理。
- **环境监测业务**：采样、检测、报告、委托、合同、仪器、质量、危废、留样销毁等模块（`Ems*` 系列接口）。
- **基础能力**：附件上传（20MB 限制）、Excel 导入导出、缓存、健康监控。

## 文档

- 产品设计：`PRD-环境监测LIMS产品设计文档.html`
- 技术需求：`TRD-环境监测LIMS技术需求文档.html`
- 需求/设计/测试/权限等：`docs/`（含 brd、prd、trd、issues、test 子目录）

## 备注

- 仓库根目录的 `_*.sql` / `_*.ps1` / `_*.txt` 为运维/数据迁移辅助脚本，非模块代码。
- 各子模块的 `dist/`、`target/`、`node_modules/` 为构建产物，请按需生成。
- 默认数据库密码、上传限制、代理地址等均为本地开发配置，生产环境务必调整。
