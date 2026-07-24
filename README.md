# 自定义流程引擎

> 一套可自定义、可扩展的企业级工作流引擎，支持可视化流程设计、表单引擎、数据模型、任务中心、会签/加签等完整审批能力。

---

## 目录

- [1. 项目概述](#1-项目概述)
- [2. 技术栈](#2-技术栈)
- [3. 项目结构](#3-项目结构)
- [4. 快速启动](#4-快速启动)
- [5. 功能模块总览](#5-功能模块总览)
- [6. 功能执行链路详解](#6-功能执行链路详解)
  - [6.1 用户认证与权限](#61-用户认证与权限)
  - [6.2 流程定义管理](#62-流程定义管理)
  - [6.3 流程设计器（可视化）](#63-流程设计器可视化)
  - [6.4 流程配置](#64-流程配置)
  - [6.5 表单引擎](#65-表单引擎)
  - [6.6 数据模型](#66-数据模型)
  - [6.7 流程发起](#67-流程发起)
  - [6.8 流程运行引擎](#68-流程运行引擎)
  - [6.9 任务中心](#69-任务中心)
  - [6.10 我的申请](#610-我的申请)
  - [6.11 会签与加签](#611-会签与加签)
  - [6.12 流程监控](#612-流程监控)
  - [6.13 后台管理](#613-后台管理)
  - [6.14 Webhook 回调](#614-webhook-回调)
  - [6.15 日志审计](#615-日志审计)
- [7. API 接口清单](#7-api-接口清单)
- [8. 数据库设计](#8-数据库设计)
- [9. 架构设计](#9-架构设计)

---

## 1. 项目概述

本项目构建了一套**完整的自定义流程引擎**，覆盖流程全生命周期：

| 能力 | 说明 |
|------|------|
| 可视化流程设计 | 基于 ReactFlow 的拖拽式流程设计器，支持 JSON 导入导出 |
| 流程运行引擎 | 解析 JSON 流程定义，自动推进节点、创建任务、处理分支 |
| 表单引擎 | JSON 驱动的表单设计器，支持多种控件、主子表、字段权限 |
| 数据模型 | 主子表结构建模，支持计算字段、表单绑定 |
| 任务中心 | 待办/已办管理、签收/转办/委派/驳回/加签 |
| 会签/加签 | 全通过/按比例/一票通过/一票否决，前/后/并行加签 |
| 后台管理 | 部门/用户/角色/权限/字典/三员管理 |
| 流程监控 | 实例状态跟踪、执行轨迹、耗时统计 |
| Webhook | 流程事件回调通知 |
| 日志审计 | 访问日志 + 操作日志，支持三员审计 |

---

## 2. 技术栈

### 后端（flow-engine）

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| Java | 17 | 运行环境 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| SQLite | 3.45.1.0 | 嵌入式数据库 |
| Caffeine | 3.1.8 | 本地缓存 |
| Groovy | 3.0.19 | 脚本任务执行 |
| Lombok | - | 代码简化 |
| Maven | - | 构建工具 |

### 前端管理后台（flow-web）

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.12 | 前端框架 |
| vue-router | 4.6.4 | 路由管理 |
| Pinia | 4.0.2 | 状态管理 |
| Ant Design Vue | 4.2.6 | UI 组件库 |
| Axios | 1.18.1 | HTTP 客户端 |
| Vite | 5.4.10 | 构建工具 |

### 流程设计器（flow-designer）

| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.3.1 | 前端框架 |
| ReactFlow | 11.11.4 | 可视化流程引擎 |
| Axios | 1.18.1 | HTTP 客户端 |
| Vite | 5.4.10 | 构建工具 |

---

## 3. 项目结构

```
workflow/
├── flow-engine/              # 后端服务（Spring Boot）
│   ├── src/main/java/com/flow/engine/
│   │   ├── FlowEngineApplication.java    # 启动入口
│   │   ├── annotation/                    # 自定义注解（@OpLog）
│   │   ├── aspect/                        # AOP 切面（操作日志）
│   │   ├── common/                        # 公共模块（Result、ErrorCode、枚举、工具类）
│   │   ├── config/                        # 配置类（缓存、数据初始化、WebMvc）
│   │   ├── controller/                    # 流程相关 API
│   │   ├── controllers/                   # 后台管理 API
│   │   ├── dto/                           # 数据传输对象
│   │   ├── engine/                        # 流程运行引擎核心
│   │   ├── entity/                        # 实体类（对应数据库表）
│   │   ├── event/                         # Spring 事件（流程/节点生命周期）
│   │   ├── interceptor/                   # 拦截器（访问日志）
│   │   ├── listener/                      # 事件监听器（Webhook、数据模型）
│   │   ├── mapper/                        # MyBatis-Plus Mapper
│   │   ├── model/                         # 流程模型（ProcessModel、NodeModel、EdgeModel）
│   │   ├── node/                          # 节点插件体系（NodeHandler 接口）
│   │   ├── parser/                        # 流程 JSON 解析器
│   │   └── service/                       # 业务服务层（23 个服务类）
│   ├── src/main/resources/
│   │   ├── application.yml                # 应用配置
│   │   └── db/schema.sql                  # 数据库建表脚本
│   └── pom.xml
├── flow-web/                  # 前端管理后台（Vue3）
│   ├── src/
│   │   ├── api/               # API 请求模块（10 个模块）
│   │   ├── components/        # 通用组件（FormRenderer、SubTableRenderer 等）
│   │   ├── composables/       # 组合式函数（权限检查）
│   │   ├── layouts/           # 布局组件（BasicLayout 侧边栏布局）
│   │   ├── router/            # 路由配置（含权限守卫）
│   │   ├── stores/            # Pinia 状态管理
│   │   ├── styles/            # 全局样式
│   │   ├── utils/             # 工具函数
│   │   ├── views/             # 页面视图（22 个页面）
│   │   ├── App.vue
│   │   └── main.js
│   └── package.json
├── flow-designer/             # 流程可视化设计器（React + ReactFlow）
│   ├── src/
│   │   ├── components/        # 设计器组件（FlowNode、ConfigPanel、NodePalette）
│   │   ├── App.jsx
│   │   ├── api.js
│   │   └── nodeTypes.js
│   └── package.json
└── docs/                      # 项目文档（BRD、PRD、TRD、ISSUE）
```

---

## 4. 快速启动

### 环境要求

- **JDK 17+**
- **Maven 3.8+**
- **Node.js 16+**（推荐 18+）
- **npm 8+**

### 4.1 启动后端服务

```bash
cd flow-engine
mvn spring-boot:run -DskipTests
```

服务启动后监听 `http://localhost:8080`，SQLite 数据库文件 `flow_engine.db` 自动生成在 `flow-engine/` 目录下。

启动时自动执行：
- `schema.sql` 建表脚本（19+ 张表）
- 数据字典初始化
- 权限数据初始化
- 三员账号初始化
- 测试数据初始化

### 4.2 启动前端管理后台

```bash
cd flow-web
npm install
npx vite --port 5173
```

访问 `http://localhost:5173`，使用初始化管理员账号登录。

### 4.3 启动流程设计器

```bash
cd flow-designer
npm install
npx vite --port 5174
```

访问 `http://localhost:5174` 打开可视化流程设计器。

---

## 5. 功能模块总览

```
自定义流程引擎
├── 工作台（Dashboard）
├── 流程管理
│   ├── 流程定义 ── 创建/编辑/部署/导入导出流程模板
│   ├── 流程实例 ── 查看运行中的流程实例状态
│   ├── 表单定义 ── 管理表单模板
│   └── 数据模型 ── 定义主子表数据结构
├── 任务中心
│   ├── 发起流程 ── 选择已部署流程并发起新实例
│   ├── 待办任务 ── 查看和处理待办任务
│   ├── 已办任务 ── 查看已处理的任务记录
│   └── 我的申请 ── 查看自己发起的流程实例
├── 后台管理
│   ├── 部门管理 ── 树形组织架构管理
│   ├── 用户管理 ── 用户账号与部门关联
│   ├── 角色管理 ── 角色定义与权限分配
│   ├── 日志管理 ── 访问日志 + 操作日志
│   ├── 数据字典 ── 字典类型与字典项管理
│   └── 三员管理 ── 系统管理员/安全管理员/审计管理员
└── 流程监控 ── 实例状态跟踪、执行轨迹、耗时统计
```

---

## 6. 功能执行链路详解

### 6.1 用户认证与权限

**功能说明**：用户登录认证、Token 管理、角色权限校验、菜单权限过滤。

**执行链路**：

```
用户访问页面
  └─→ 路由守卫 router.beforeEach()
        ├─→ 检查 localStorage.token 是否存在
        ├─→ 无 Token → 重定向 /login
        └─→ 有 Token → 检查 permKey 权限
              ├─→ isAdmin=true → 直接放行
              ├─→ permissions 包含 permKey → 放行
              └─→ 无权限 → 重定向 /dashboard

登录流程:
  前端 Login 页面
    └─→ POST /api/v1/auth/login {username, password}
          └─→ AuthService.login()
                ├─→ 查询 User 表验证用户名密码
                ├─→ 生成 Token 存入内存 Session
                └─→ 返回 {token}
    └─→ GET /api/v1/auth/info（携带 Token）
          └─→ AuthService.validateToken()
                ├─→ 返回 userId、username、roles、permissions、isAdmin
                └─→ 前端存入 Pinia userStore + localStorage
    └─→ BasicLayout 根据 permissions 过滤侧边栏菜单
```

**涉及文件**：
- 后端：`AuthController.java`、`AuthService.java`、`RolePermissionService.java`
- 前端：`views/login/index.vue`、`stores/user.js`、`router/index.js`、`layouts/BasicLayout.vue`

---

### 6.2 流程定义管理

**功能说明**：创建、编辑、删除、部署、取消部署、导入导出流程定义模板。

**执行链路**：

```
流程定义列表页
  └─→ GET /api/v1/process/definitions?processKey=&category=&status=
        └─→ ProcessDefinitionService.list()
              └─→ 查询 wf_process_definition 表 → 返回列表

创建流程定义:
  前端表单填写（名称、Key、分类、JSON定义）
    └─→ POST /api/v1/process/definitions
          └─→ ProcessDefinitionService.create()
                ├─→ 校验 processKey 唯一性
                ├─→ 保存至 wf_process_definition（status=0 草稿）
                └─→ 返回创建结果

部署流程:
  列表页点击"部署"按钮
    └─→ POST /api/v1/process/definitions/{id}/deploy
          └─→ ProcessDefinitionService.deploy()
                ├─→ 校验流程 JSON 合法性
                ├─→ 更新 status=1（已部署）
                └─→ 部署后可被发起流程选用

导入/导出:
  导出: GET /api/v1/process/definitions/{id}/export → 返回完整 JSON
  导入: POST /api/v1/process/definitions/import → 从 JSON 创建新定义
```

**涉及文件**：
- 后端：`ProcessController.java`、`ProcessDefinitionService.java`
- 前端：`views/process/definition/index.vue`、`api/process.js`

---

### 6.3 流程设计器（可视化）

**功能说明**：基于 ReactFlow 的拖拽式可视化流程设计器，支持节点拖放、连线、属性配置。

**执行链路**：

```
打开设计器页面（flow-designer 独立应用）
  └─→ App.jsx 初始化 ReactFlow 画布
        ├─→ 加载已有流程定义 JSON → 渲染节点和连线
        └─→ 空画布 → 从左侧面板拖放节点

设计器三栏布局:
  ┌───────────┬──────────────────┬───────────┐
  │ NodePalette│   ReactFlow 画布  │ConfigPanel│
  │ (节点面板) │  (拖拽/连线区域)  │ (属性配置)│
  └───────────┴──────────────────┴───────────┘

节点操作:
  拖放节点 → 创建 FlowNode 实例 → 注册到 ReactFlow
  连线操作 → 创建 Edge → 定义节点流转关系
  点击节点 → ConfigPanel 展示节点属性配置
    ├─→ 节点类型（start/end/userTask/exclusiveGateway/parallelGateway 等）
    ├─→ 处理人配置（assigneeType: user/role/deptLeader/initiator）
    ├─→ 表单绑定（formKey）
    ├─→ 条件表达式（网关分支条件）
    └─→ 字段权限配置

保存设计:
  点击"保存" → 将 ReactFlow 节点/连线序列化为 JSON
    └─→ PUT /api/v1/process/definitions/{id}
          └─→ 更新 process_json 字段
```

**支持节点类型**：

| 节点类型 | 说明 |
|---------|------|
| start | 开始节点 |
| end | 结束节点 |
| userTask | 用户任务节点（需人工审批） |
| serviceTask | 服务任务节点（自动执行） |
| exclusiveGateway | 排他网关（条件分支） |
| parallelGateway | 并行网关（并行执行） |
| inclusiveGateway | 包容网关 |
| scriptTask | 脚本任务（Groovy 执行） |

**涉及文件**：
- `flow-designer/src/App.jsx`、`components/FlowNode.jsx`、`components/ConfigPanel.jsx`、`components/NodePalette.jsx`、`nodeTypes.js`

---

### 6.4 流程配置

**功能说明**：对流程定义进行高级配置，包括节点表单权限、字段权限、按钮权限、审批策略等。

**执行链路**：

```
流程配置页面
  └─→ 加载流程定义详情 + 关联表单定义
        ├─→ GET /api/v1/process/definitions/{id}
        └─→ GET /api/v1/forms/{formKey}

配置内容:
  ├─→ 节点表单权限
  │     ├─→ mode: edit / readonly / hidden
  │     ├─→ allowSaveDraft / allowWithdraw / allowCancel
  │     └─→ 字段级权限: edit / readonly / hidden / required
  ├─→ 处理人配置
  │     ├─→ assigneeType: user / role / deptLeader / initiator
  │     └─→ 支持多处理人（user 分配方式）
  ├─→ 审批策略
  │     ├─→ 会签模式: allPass / ratio / onePass / oneReject
  │     └─→ 加签配置: before / after / parallel
  └─→ 保存配置 → PUT /api/v1/process/definitions/{id}
```

**涉及文件**：
- 后端：`ProcessController.java`、`FormPermissionService.java`
- 前端：`views/process/config/index.vue`

---

### 6.5 表单引擎

**功能说明**：JSON 驱动的表单设计器，支持多种控件、三栏布局、主子表、数据模型绑定。

**执行链路**：

```
表单定义管理:
  列表页 → GET /api/v1/forms → FormDefinitionService.list()
  创建表单 → POST /api/v1/forms → FormDefinitionService.create()

表单设计器:
  前端 FormDesign 页面
    ├─→ 左侧组件面板（控件拖放）
    │     ├─→ 基础控件: 文本、数字、日期、下拉、单选、多选
    │     ├─→ 高级控件: 人员选择器、部门选择器、级联选择
    │     ├─→ 布局控件: 分组面板、子表
    │     └─→ 计算控件: 支持公式（SUM/AVG 等）
    ├─→ 中间画布（表单预览/布局）
    │     └─→ 支持三栏布局（sections 嵌套结构）
    └─→ 右侧属性配置
          ├─→ 字段属性: label、placeholder、required、校验规则
          ├─→ 数据绑定: 绑定数据模型字段
          └─→ 子表配置: 行增删改权限

表单渲染:
  FormRenderer 组件解析 sections 嵌套结构
    ├─→ 读取 formJson → 递归渲染各 section
    ├─→ SubTableRenderer 处理子表渲染
    ├─→ 字段权限应用（edit/readonly/hidden）
    └─→ 数据双向绑定（v-model）

表单数据存储:
  提交表单 → POST /api/v1/forms/{formKey}/data
    └─→ FormDefinitionService.saveFormData()
          └─→ 数据持久化到 wf_form_data
```

**涉及文件**：
- 后端：`FormController.java`、`FormDefinitionService.java`、`FormPermissionService.java`
- 前端：`views/form/definition/index.vue`、`views/form/design/index.vue`、`components/FormRenderer.vue`、`components/SubTableRenderer.vue`

---

### 6.6 数据模型

**功能说明**：定义主子表结构的业务数据模型，支持计算字段、表单绑定、独立 CRUD。

**执行链路**：

```
数据模型管理:
  列表页 → GET /api/v1/data-models → DataModelService.list()
  创建模型 → POST /api/v1/data-models
    └─→ DataModelService.create()
          ├─→ 定义主表字段（text/number/date/user/dept/amount 等）
          ├─→ 定义子表（一对多关系）
          ├─→ 配置计算字段（如 SUM(detail.amount)）
          └─→ 保存至 wf_data_model

模型实例操作:
  创建实例 → POST /api/v1/data-models/{modelKey}/instances
    └─→ ModelInstanceManager.create()
          ├─→ 主表数据写入 wf_model_instance
          └─→ 子表数据写入关联记录
  查询实例 → GET /api/v1/data-models/instances/{instanceId}
  更新实例 → PUT /api/v1/data-models/instances/{instanceId}

与流程集成:
  流程发起 → 自动创建模型实例
  节点审批 → 读取/更新模型数据
  流程完成 → 归档模型实例
```

**涉及文件**：
- 后端：`DataModelController.java`、`DataModelService.java`、`ModelInstanceManager.java`、`DataModelProcessListener.java`
- 前端：`views/data-model/index.vue`、`api/model.js`

---

### 6.7 流程发起

**功能说明**：从任务中心选择已部署的流程定义，填写业务信息后发起流程实例。

**执行链路**：

```
发起流程列表页 (/task/start)
  └─→ GET /api/v1/process/definitions?status=1
        └─→ 按 category 分组展示已部署流程
              ├─→ 流程卡片展示（名称、描述、版本、processKey）
              └─→ 支持关键词模糊搜索

点击流程卡片 → 跳转发起详情页 (/task/start-detail)
  └─→ 加载流程定义详情 + 关联表单
        ├─→ GET /api/v1/process/definitions/{id}
        └─→ 渲染表单（FormRenderer 组件）

填写表单并提交:
  前端收集表单数据 + 流程变量
    └─→ POST /api/v1/process/instances
          └─→ ProcessInstanceService.start()
                └─→ FlowEngine.startProcess()
                      ├─→ 1. 查找已部署的流程定义
                      ├─→ 2. 解析流程 JSON（ProcessJsonParser）
                      ├─→ 3. 创建流程实例（wf_process_instance）
                      │     └─→ 生成编号: {processKey}-{yyyyMMdd}-{4位序号}
                      ├─→ 4. 保存流程变量（wf_variable）
                      ├─→ 5. 发布 ProcessStartedEvent
                      └─→ 6. 从 start 节点开始执行
                            └─→ executeFromStart() → executeFromNode()
                                  └─→ 自动推进到第一个 userTask 节点
                                        └─→ 创建任务（wf_task）→ 等待审批
```

**涉及文件**：
- 后端：`ProcessInstanceController.java`、`ProcessInstanceService.java`、`FlowEngine.java`
- 前端：`views/task/start.vue`、`views/task/start-detail.vue`、`components/FormRenderer.vue`

---

### 6.8 流程运行引擎

**功能说明**：流程引擎核心，负责解析流程定义、执行节点逻辑、处理流转、管理实例生命周期。

**执行链路**：

```
FlowEngine 核心流程:

  startProcess(processKey, businessKey, startUser, variables)
    ├─→ 校验流程定义已部署
    ├─→ ProcessJsonParser.parse(processJson) → ProcessModel
    ├─→ 创建 ProcessInstance 记录
    ├─→ VariableService.saveVariables() 保存变量
    ├─→ publishEvent(ProcessStartedEvent)
    └─→ executeFromStart(context, model)
          └─→ 找到 start 节点 → executeFromNode()

  executeFromNode(context, model, currentNode)
    └─→ while 循环（最大 100 步防死循环）
          ├─→ NodeExecutor.execute(context, node, model)
          │     ├─→ start 节点 → 直接完成 → moveTo 下一节点
          │     ├─→ userTask 节点 → 创建 Task → 返回 waiting
          │     ├─→ serviceTask 节点 → 自动执行 → moveTo
          │     ├─→ exclusiveGateway → 评估条件表达式 → 选择分支
          │     ├─→ parallelGateway → 并行执行所有分支
          │     ├─→ scriptTask → Groovy 脚本执行
          │     └─→ end 节点 → 返回 completed
          ├─→ result.isWaiting() → 停止等待外部触发
          ├─→ result.isCompleted() → completeProcess()
          └─→ result.isMoveTo() → 继续下一节点

  completeTask(instanceId, variables)
    ├─→ 合并变量到流程上下文
    ├─→ 解析流程定义获取下一节点
    │     └─→ ProcessJsonParser.getNextNode(model, currentNodeId, vars)
    ├─→ 触发当前节点 afterComplete 事件
    ├─→ 有下一节点 → executeFromNode() 继续推进
    └─→ 无下一节点 → completeProcess()

  rollback(instanceId, targetNodeId)
    ├─→ 回退实例到指定节点
    ├─→ 触发 afterReject 事件
    └─→ 从目标节点重新执行

  completeProcess(instance)
    ├─→ 更新状态为 COMPLETED
    ├─→ 记录结束时间和耗时
    └─→ 发布 ProcessCompletedEvent
```

**核心类**：
- `FlowEngine.java` — 引擎核心（发起、完成、回退）
- `NodeExecutor.java` — 节点执行器（分发到各类节点处理器）
- `ProcessJsonParser.java` — JSON 解析器（解析节点/连线/条件）
- `ExecutionContext.java` — 执行上下文（变量作用域管理）
- `VariableService.java` — 流程变量管理

---

### 6.9 任务中心

**功能说明**：管理用户的待办/已办任务，支持签收、完成、驳回、转办、委派等操作。

**执行链路**：

```
待办任务列表 (/task/todo)
  └─→ GET /api/v1/tasks/todo?userId=xxx
        └─→ TaskService.getTodoList()
              └─→ 查询 wf_task 表（status=PENDING/CLAIMED）

任务办理 (/task/handle):
  点击待办任务 → 跳转独立办理页面
    ├─→ GET /api/v1/tasks/{id} → 获取任务详情
    ├─→ GET /api/v1/tasks/{taskId}/form-permissions → 获取表单权限
    ├─→ 加载关联表单（FormRenderer 渲染）
    │     └─→ 按权限控制字段可编辑/只读/隐藏
    └─→ 显示流程图（节点状态高亮）

审批操作:
  同意（Complete）:
    └─→ POST /api/v1/tasks/{id}/complete {userId, variables}
          └─→ TaskService.complete()
                ├─→ 更新任务状态为 COMPLETED
                ├─→ 记录审批意见
                └─→ FlowEngine.completeTask() → 推进到下一节点

  驳回（Reject）:
    └─→ POST /api/v1/tasks/{id}/reject {userId, targetNodeId, variables}
          └─→ TaskService.reject()
                ├─→ 更新任务状态为 REJECTED
                └─→ FlowEngine.rollback() → 回退到指定节点

  签收（Claim）:
    └─→ POST /api/v1/tasks/{id}/claim {userId}
          └─→ TaskService.claim() → 更新 assignee

  取消签收（Unclaim）:
    └─→ POST /api/v1/tasks/{id}/unclaim
          └─→ TaskService.unclaim() → 清空 assignee

  转办（Transfer）:
    └─→ POST /api/v1/tasks/{id}/transfer {operatorId, targetUserId}
          └─→ TaskService.transfer() → 变更处理人

  委派（Delegate）:
    └─→ POST /api/v1/tasks/{id}/delegate {operatorId, delegateUserId}
          └─→ TaskService.delegate() → 委派处理

已办任务列表 (/task/done):
  └─→ GET /api/v1/tasks/done?userId=xxx
        └─→ TaskService.getDoneList()
              └─→ 查询已完成的任务记录
```

**涉及文件**：
- 后端：`TaskController.java`、`TaskService.java`
- 前端：`views/task/todo.vue`、`views/task/handle.vue`、`views/task/done.vue`、`api/task.js`

---

### 6.10 我的申请

**功能说明**：查看当前用户发起的所有流程实例。

**执行链路**：

```
我的申请页面 (/task/my-request)
  └─→ GET /api/v1/process/instances?startUser=xxx
        └─→ ProcessInstanceService.list()
              └─→ 查询 wf_process_instance（按 start_user 过滤）
                    ├─→ 展示实例列表（编号、名称、状态、发起时间）
                    ├─→ 状态: RUNNING/COMPLETED/TERMINATED/SUSPENDED
                    └─→ 支持查看详情和流程轨迹
```

**涉及文件**：
- 后端：`ProcessInstanceController.java`、`ProcessInstanceService.java`
- 前端：`views/task/my-request.vue`

---

### 6.11 会签与加签

**功能说明**：支持多人会签审批和审批过程中临时加签。

**执行链路**：

```
会签模式:
  节点配置 signType=counterSign + signMode=allPass/ratio/onePass/oneReject

  进入会签节点:
    └─→ NodeExecutor 检测到会签配置
          ├─→ 创建 CounterSignGroup（会签组）
          ├─→ 为每个审批人创建 Task
          └─→ 任务状态均为 PENDING

  会签投票:
    └─→ POST /api/v1/tasks/{taskId}/counter-sign/vote
          └─→ TaskService.vote()
                ├─→ 记录 CounterSignVote（同意/反对/意见）
                ├─→ 汇总投票结果
                └─→ 根据 signMode 判断是否满足通过条件
                      ├─→ allPass: 全部同意 → 通过
                      ├─→ ratio: 同意比例 ≥ 设定值 → 通过
                      ├─→ onePass: 任一同意 → 通过
                      └─→ oneReject: 任一拒绝 → 驳回

加签模式:
  审批过程中发起加签:
    └─→ POST /api/v1/tasks/{id}/add-sign {type, userIds}
          └─→ TaskService.addSign()
                ├─→ type=before: 新增审批人在原审批人之前
                ├─→ type=after: 新增审批人在原审批人之后
                ├─→ type=parallel: 新增审批人并行处理
                └─→ 创建新 Task 记录
```

**涉及文件**：
- 后端：`TaskController.java`、`TaskService.java`、`CounterSignGroup.java`、`CounterSignVote.java`
- DTO：`AddSignRequest.java`、`VoteRequest.java`、`CounterSignResultResponse.java`

---

### 6.12 流程监控

**功能说明**：管理员查看流程实例运行状态、执行轨迹、耗时统计。

**执行链路**：

```
流程监控页面 (/monitor)
  └─→ GET /api/v1/monitor/instances
        └─→ ProcessMonitorService.getInstances()
              ├─→ 查询所有流程实例（含状态统计）
              ├─→ 运行中/已完成/已终止/已暂停数量
              └─→ 平均耗时统计

实例详情:
  └─→ GET /api/v1/monitor/instances/{id}
        └─→ ProcessMonitorService.getInstanceDetail()
              ├─→ 实例基本信息
              ├─→ 节点执行轨迹（NodeExecution 列表）
              ├─→ 各节点耗时
              └─→ 流程变量快照

管理操作:
  暂停: POST /api/v1/process/instances/{id}/suspend
  恢复: POST /api/v1/process/instances/{id}/resume
  终止: POST /api/v1/process/instances/{id}/terminate
```

**涉及文件**：
- 后端：`MonitorController.java`、`ProcessMonitorService.java`、`ProcessInstanceController.java`
- 前端：`views/monitor/index.vue`、`api/monitor.js`

---

### 6.13 后台管理

#### 6.13.1 部门管理

**功能说明**：树形组织架构管理，支持部门增删改、排序、停用、设置部门领导。

```
部门管理页面 (/system/dept)
  ├─→ GET /api/v1/depts → DeptService.list() → 树形结构展示
  ├─→ POST /api/v1/depts → 创建部门
  ├─→ PUT /api/v1/depts/{id} → 编辑部门
  ├─→ DELETE /api/v1/depts/{id} → 删除部门
  └─→ PUT /api/v1/depts/{id}/leaders → 设置部门领导（正/副负责人）
```

**涉及文件**：`DeptController.java`、`DeptService.java`、`views/system/dept.vue`

#### 6.13.2 用户管理

**功能说明**：用户账号管理，关联部门（主部门+兼职），角色分配，密级设置。

```
用户管理页面 (/system/user)
  ├─→ GET /api/v1/users → UserService.list()
  ├─→ POST /api/v1/users → 创建用户（含密码加密）
  ├─→ PUT /api/v1/users/{id} → 编辑用户
  ├─→ DELETE /api/v1/users/{id} → 删除用户
  ├─→ POST /api/v1/users/{id}/roles → 分配角色
  └─→ 支持主部门 + 兼职部门关联
```

**涉及文件**：`UserController.java`、`UserService.java`、`views/system/user.vue`

#### 6.13.3 角色管理

**功能说明**：角色定义与权限分配。

```
角色管理页面 (/system/role)
  ├─→ GET /api/v1/roles → RoleService → 角色列表
  ├─→ POST /api/v1/roles → 创建角色
  ├─→ PUT /api/v1/roles/{id} → 编辑角色
  ├─→ DELETE /api/v1/roles/{id} → 删除角色
  └─→ POST /api/v1/roles/{id}/permissions → 分配权限（菜单/按钮/操作）
```

**涉及文件**：`RoleController.java`、`RolePermissionService.java`、`views/system/role.vue`

#### 6.13.4 数据字典

**功能说明**：维护系统字典类型和字典项，驱动下拉选择等场景。

```
数据字典页面 (/system/dict)
  ├─→ 左侧: 字典类型列表
  │     ├─→ GET /api/v1/dict/types
  │     ├─→ POST/PUT/DELETE /api/v1/dict/types/{id}
  │     └─→ 内置: 流程分类、表单分类等
  └─→ 右侧: 字典项列表（选中类型后加载）
        ├─→ GET /api/v1/dict/types/{typeId}/items
        └─→ POST/PUT/DELETE /api/v1/dict/items/{id}
```

**涉及文件**：`DictController.java`、`DictService.java`、`DictDataInitializer.java`、`views/system/dict.vue`

#### 6.13.5 三员管理

**功能说明**：系统管理员/安全管理员/审计管理员的权限分离管理。

```
三员管理页面 (/system/admin)
  └─→ TripleAdminService + TripleAdminPermissionEvaluator
        ├─→ 系统管理员: 用户管理、基础数据维护
        ├─→ 安全管理员: 角色权限分配、安全策略
        ├─→ 审计管理员: 日志查看和审计
        └─→ 三员互斥: 不能互相操作、不能删除自己账号
```

**涉及文件**：`AdminController.java`、`TripleAdminService.java`、`TripleAdminPermissionEvaluator.java`、`TripleAdminInitializer.java`

---

### 6.14 Webhook 回调

**功能说明**：配置 Webhook 监听流程事件，当事件触发时自动发送 HTTP 回调。

**执行链路**：

```
配置 Webhook:
  └─→ POST /api/v1/webhooks → WebhookService.create()
        ├─→ 配置 URL、事件类型、请求头、重试策略
        └─→ 保存至 wf_webhook

事件触发:
  流程事件发生 → Spring Event 发布
    └─→ WebhookEventListener 监听事件
          └─→ WebhookScheduler 调度回调
                ├─→ 匹配事件类型的 Webhook
                ├─→ 发送 HTTP POST 请求
                ├─→ 记录回调日志（wf_webhook_log）
                └─→ 失败重试（按配置策略）

查看日志:
  └─→ GET /api/v1/webhooks/{id}/logs → 回调日志列表
```

**涉及文件**：`WebhookController.java`、`WebhookService.java`、`WebhookScheduler.java`、`WebhookEventListener.java`

---

### 6.15 日志审计

**功能说明**：记录用户访问日志和业务操作日志，支持审计查询。

**执行链路**：

```
访问日志:
  用户请求 → AccessLogInterceptor 拦截
    ├─→ 记录 URL、方法、IP、User-Agent、耗时
    ├─→ 写入 wf_access_log
    └─→ 异步写入不影响业务性能

操作日志:
  业务方法标注 @OpLog 注解
    └─→ OperationLogAspect AOP 切面
          ├─→ 记录操作类型、模块、描述、操作人、IP
          ├─→ 记录变更前后的数据快照
          └─→ 写入 wf_operation_log

日志查询:
  └─→ GET /api/v1/logs/access → 访问日志列表
  └─→ GET /api/v1/logs/operation → 操作日志列表
  └─→ 支持按时间、用户、模块筛选
```

**涉及文件**：`LogController.java`、`LogService.java`、`AccessLogInterceptor.java`、`OperationLogAspect.java`、`@OpLog`

---

## 7. API 接口清单

### 7.1 认证 API（/api/v1/auth）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/auth/login | 用户登录 |
| POST | /api/v1/auth/logout | 用户登出 |
| GET | /api/v1/auth/info | 获取当前用户信息 |

### 7.2 流程定义 API（/api/v1/process/definitions）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/process/definitions | 获取流程定义列表 |
| GET | /api/v1/process/definitions/{id} | 获取流程定义详情 |
| GET | /api/v1/process/definitions/key/{processKey} | 按 Key 获取最新版本 |
| POST | /api/v1/process/definitions | 创建流程定义 |
| PUT | /api/v1/process/definitions/{id} | 更新流程定义 |
| DELETE | /api/v1/process/definitions/{id} | 删除流程定义 |
| POST | /api/v1/process/definitions/{id}/deploy | 部署流程 |
| POST | /api/v1/process/definitions/{id}/undeploy | 取消部署 |
| GET | /api/v1/process/definitions/{id}/export | 导出流程定义 |
| POST | /api/v1/process/definitions/import | 导入流程定义 |

### 7.3 流程实例 API（/api/v1/process/instances）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/process/instances | 获取流程实例列表 |
| GET | /api/v1/process/instances/{id} | 获取实例详情 |
| POST | /api/v1/process/instances | 发起流程实例 |
| POST | /api/v1/process/instances/{id}/suspend | 暂停实例 |
| POST | /api/v1/process/instances/{id}/resume | 恢复实例 |
| POST | /api/v1/process/instances/{id}/terminate | 终止实例 |

### 7.4 任务 API（/api/v1/tasks）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/tasks/todo?userId=xxx | 获取待办任务 |
| GET | /api/v1/tasks/done?userId=xxx | 获取已办任务 |
| GET | /api/v1/tasks/{id} | 获取任务详情 |
| GET | /api/v1/tasks/instance/{instanceId} | 获取实例所有任务 |
| POST | /api/v1/tasks/{id}/claim | 签收任务 |
| POST | /api/v1/tasks/{id}/unclaim | 取消签收 |
| POST | /api/v1/tasks/{id}/complete | 完成任务 |
| POST | /api/v1/tasks/{id}/reject | 驳回任务 |
| POST | /api/v1/tasks/{id}/transfer | 转办任务 |
| POST | /api/v1/tasks/{id}/delegate | 委派任务 |
| POST | /api/v1/tasks/{id}/add-sign | 加签 |
| GET | /api/v1/tasks/{taskId}/form-permissions | 获取表单权限 |

### 7.5 表单 API（/api/v1/forms）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/forms | 表单定义列表 |
| GET | /api/v1/forms/{formKey} | 获取表单详情 |
| POST | /api/v1/forms | 创建表单 |
| PUT | /api/v1/forms/{id} | 更新表单 |
| POST | /api/v1/forms/{formKey}/data | 提交表单数据 |

### 7.6 数据模型 API（/api/v1/data-models）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/data-models | 获取模型列表 |
| GET | /api/v1/data-models/{modelKey} | 获取模型详情 |
| POST | /api/v1/data-models | 创建模型 |
| PUT | /api/v1/data-models/{modelKey} | 更新模型 |
| DELETE | /api/v1/data-models/{modelKey} | 删除模型 |
| POST | /api/v1/data-models/{modelKey}/instances | 创建模型实例 |
| GET | /api/v1/data-models/instances/{instanceId} | 获取实例数据 |
| PUT | /api/v1/data-models/instances/{instanceId} | 更新实例数据 |

### 7.7 后台管理 API

| 模块 | 基础路径 | CRUD 操作 |
|------|---------|-----------|
| 部门 | /api/v1/depts | GET/POST/PUT/DELETE + 设置领导 |
| 用户 | /api/v1/users | GET/POST/PUT/DELETE + 角色分配 |
| 角色 | /api/v1/roles | GET/POST/PUT/DELETE + 权限分配 |
| 权限 | /api/v1/permissions | GET/POST/PUT/DELETE |
| 字典类型 | /api/v1/dict/types | GET/POST/PUT/DELETE |
| 字典项 | /api/v1/dict/items | GET/POST/PUT/DELETE |
| 日志 | /api/v1/logs/access + /operation | GET（查询+筛选） |
| Webhook | /api/v1/webhooks | GET/POST/PUT/DELETE + 日志 |

### 7.8 监控 API（/api/v1/monitor）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/monitor/instances | 获取监控实例列表 |
| GET | /api/v1/monitor/instances/{id} | 实例详情（含轨迹） |
| GET | /api/v1/monitor/statistics | 统计数据 |

---

## 8. 数据库设计

数据库使用 SQLite，通过 `schema.sql` 自动建表，共 19+ 张表：

### 流程核心表（wf_ 前缀）

| 表名 | 说明 |
|------|------|
| wf_process_definition | 流程定义（模板） |
| wf_process_instance | 流程实例 |
| wf_task | 任务 |
| wf_variable | 流程/任务变量 |
| wf_form_definition | 表单定义 |
| wf_data_model | 数据模型 |
| wf_model_instance | 模型实例 |
| wf_counter_sign_group | 会签组 |
| wf_counter_sign_vote | 会签投票 |
| wf_webhook | Webhook 配置 |
| wf_webhook_log | Webhook 日志 |

### 系统管理表（sys_ 前缀）

| 表名 | 说明 |
|------|------|
| sys_user | 用户 |
| sys_dept | 部门 |
| sys_role | 角色 |
| sys_permission | 权限 |
| sys_user_role | 用户-角色关联 |
| sys_role_permission | 角色-权限关联 |
| sys_user_post | 用户岗位（兼职） |
| sys_data_permission | 数据权限 |
| sys_dict_type | 字典类型 |
| sys_dict_item | 字典项 |
| sys_access_log | 访问日志 |
| sys_operation_log | 操作日志 |

---

## 9. 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                       表现层                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ flow-web     │  │ flow-designer│  │ REST API         │  │
│  │ Vue3+Antd    │  │ React+Flow   │  │ (第三方集成)      │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼─────────────────┼───────────────────┼────────────┘
          │                 │                   │
          ▼                 ▼                   ▼
┌─────────────────────────────────────────────────────────────┐
│                     应用层（Spring Boot）                     │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ 流程定义服务 │  │ 流程运行引擎 │  │ 任务中心服务      │   │
│  │ ProcessDef  │  │ FlowEngine   │  │ TaskService      │   │
│  │ Service     │  │ NodeExecutor │  │ FormPermission   │   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │ 表单服务    │  │ 数据模型服务 │  │ Webhook 服务     │   │
│  │ FormService │  │ DataModel    │  │ WebhookScheduler │   │
│  └─────────────┘  └──────────────┘  └──────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 后台管理: 用户/部门/角色/权限/字典/三员/日志         │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     基础设施层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ SQLite       │  │ Caffeine     │  │ Spring Event     │  │
│  │ (嵌入式DB)   │  │ (本地缓存)   │  │ (事件驱动)       │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**事件驱动机制**：
- `ProcessStartedEvent` — 流程启动时发布
- `ProcessCompletedEvent` — 流程完成时发布
- `NodeEnteredEvent` — 进入节点时发布
- `NodeCompletedEvent` — 节点完成时发布
- `WebhookEventListener` — 监听事件触发 Webhook 回调
- `DataModelProcessListener` — 监听事件处理数据模型联动

**节点插件体系**：
- `NodeHandler` 接口 — 定义节点生命周期（onEnter/execute/onLeave）
- `AbstractNodeHandler` — 抽象基类
- `NodeHandlerAutoConfiguration` — Spring Bean 自动发现注册
- 支持自定义节点扩展

---

> **文档版本**: V1.0 | **更新日期**: 2026-07-24
