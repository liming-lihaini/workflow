# 技术需求说明书（TRD）

## 封面

| **项目名称** | 自定义流程引擎 |
|---|---|
| **文档标题** | 自定义流程引擎技术需求说明书 |
| **编制日期** | 2026-07-14 |
| **编制人** | 技术团队 |

---

## 版本历史

| 版本 | 日期 | 变更说明 | 编制人 |
|------|------|---------|--------|
| V1.0 | 2026-07-14 | 初始版本 | 技术团队 |

---

## 变更记录

| 变更日期 | 变更内容 | 变更人 |
|----------|---------|--------|
| - | - | - |

---

## 1. 技术架构设计

### 1.1 整体技术架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           技术架构分层                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        表现层 (Presentation)                       │  │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐    │  │
│  │   │  Web管理后台  │  │  REST API   │  │   前端可视化设计器   │    │  │
│  │   │  (Vue3+AntD) │  │  (Spring Boot) │   (React+ReactFlow) │    │  │
│  │   └─────────────┘  └─────────────┘  └─────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                    │                                      │
│                                    ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                        应用层 (Application)                       │  │
│  │   ┌─────────────────────────────────────────────────────────────┐  │  │
│  │   │                    Spring Boot 应用                         │  │  │
│  │   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │  │  │
│  │   │  │流程设计服务  │  │ 流程执行引擎 │  │   任务中心   │    │  │  │
│  │   │  │ProcessDefinition│ │  FlowEngine │  │   TaskService│    │  │  │
│  │   │  │  Service     │  │             │  │             │    │  │  │
│  │   │  └─────────────┘  └─────────────┘  └─────────────┘    │  │  │
│  │   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │  │  │
│  │   │  │   表单服务   │  │  节点插件   │  │   监控服务   │    │  │  │
│  │   │  │FormDefinition│  │NodeExecutor │  │              │    │  │  │
│  │   │  │   Service   │  │             │  │              │    │  │  │
│  │   │  └─────────────┘  └─────────────┘  └─────────────┘    │  │  │
│  │   └─────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                    │                                      │
│                                    ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         领域层 (Domain)                           │  │
│  │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │  │
│  │   │  流程定义域  │  │ 流程实例域   │  │    任务域    │          │  │
│  │   │ProcessDef   │  │ProcessInstance│  │    Task    │          │  │
│  │   │   Domain    │  │   Domain     │  │   Domain   │          │  │
│  │   └─────────────┘  └─────────────┘  └─────────────┘          │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                    │                                      │
│                                    ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    基础设施层 (Infrastructure)                    │  │
│  │   ┌─────────────┐                                              │  │
│  │   │   SQLite    │                                              │  │
│  │   │   数据库存储  │                                              │  │
│  │   └─────────────┘                                              │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 技术选型

| 层级 | 技术选型 | 版本 | 说明 |
|------|---------|------|------|
| 后端框架 | Spring Boot | 3.x | Java生态主流框架 |
| JDK | JDK | 17 | Java开发工具包 |
| 数据库 | SQLite | 3.x | 轻量级嵌入式数据库 |
| 前端框架 | Vue3 | 3.4+ | 管理后台界面 |
| UI组件库 | Ant Design Vue | 4.x | 企业级UI组件 |
| 流程设计器 | React Flow | 11.x | 可视化流程设计 |
| ORM框架 | MyBatis Plus | 3.5.x | 数据访问层 |

---

## 2. 数据库设计

### 2.1 核心表结构

#### 2.1.1 流程定义表 (wf_process_definition)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| process_key | VARCHAR(64) | 流程定义Key |
| process_name | VARCHAR(128) | 流程定义名称 |
| version | INT | 版本号 |
| process_json | LONGTEXT | 流程定义JSON |
| category | VARCHAR(64) | 分类 |
| status | TINYINT | 状态：0-草稿，1-已部署 |
| deployment_id | VARCHAR(64) | 部署ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | VARCHAR(64) | 创建人 |

#### 2.1.2 流程实例表 (wf_process_instance)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| process_key | VARCHAR(64) | 流程定义Key |
| process_name | VARCHAR(128) | 流程名称 |
| process_version | INT | 流程版本 |
| business_key | VARCHAR(128) | 业务主键 |
| status | TINYINT | 状态：0-运行中，1-已完成，2-已暂停，3-已终止 |
| start_user | VARCHAR(64) | 发起人 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration | BIGINT | 运行时长(毫秒) |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.3 任务表 (wf_task)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| process_instance_id | BIGINT | 流程实例ID |
| process_key | VARCHAR(64) | 流程定义Key |
| node_id | VARCHAR(64) | 节点ID |
| node_name | VARCHAR(128) | 节点名称 |
| task_type | TINYINT | 任务类型：1-普通任务，2-会签任务，3-加签任务 |
| assignee | VARCHAR(64) | 处理人 |
| candidate_users | VARCHAR(512) | 候选人(逗号分隔) |
| claim_time | DATETIME | 签收时间 |
| complete_time | DATETIME | 完成时间 |
| status | TINYINT | 状态：0-待处理，1-处理中，2-已完成 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.4 表单定义表 (wf_form_definition)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| form_key | VARCHAR(64) | 表单Key |
| form_name | VARCHAR(128) | 表单名称 |
| form_json | LONGTEXT | 表单定义JSON |
| category | VARCHAR(64) | 分类 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.5 数据模型表 (wf_data_model)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| model_key | VARCHAR(64) | 模型Key |
| model_name | VARCHAR(128) | 模型名称 |
| model_json | LONGTEXT | 模型定义JSON |
| version | INT | 版本号 |
| status | TINYINT | 状态：0-草稿，1-已发布 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.6 模型实例表 (wf_model_instance)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| model_key | VARCHAR(64) | 模型Key |
| model_instance_id | VARCHAR(64) | 模型实例ID |
| process_instance_id | BIGINT | 关联流程实例ID |
| data_json | LONGTEXT | 实例数据JSON |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.7 流程变量表 (wf_variable)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| process_instance_id | BIGINT | 流程实例ID |
| task_id | BIGINT | 任务ID（可为空） |
| variable_key | VARCHAR(64) | 变量Key |
| variable_value | LONGTEXT | 变量值 |
| variable_type | VARCHAR(32) | 变量类型 |
| create_time | DATETIME | 创建时间 |

#### 2.1.8 部门表 (sys_dept)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| parent_id | BIGINT | 父部门ID |
| dept_name | VARCHAR(64) | 部门名称 |
| dept_code | VARCHAR(32) | 部门编码 |
| dept_type | VARCHAR(32) | 部门类型 |
| sort_order | INT | 排序 |
| leader_id | BIGINT | 负责人ID |
| leader_name | VARCHAR(64) | 负责人姓名 |
| phone | VARCHAR(20) | 联系电话 |
| status | TINYINT | 状态：0-停用，1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.9 用户表 (sys_user)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| username | VARCHAR(64) | 用户名 |
| password | VARCHAR(128) | 密码 |
| real_name | VARCHAR(64) | 真实姓名 |
| email | VARCHAR(128) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| dept_id | BIGINT | 主部门ID |
| post_id | BIGINT | 岗位ID |
| security_level | TINYINT | 密级：1-公开，2-内部，3-秘密，4-机密 |
| status | TINYINT | 状态：0-停用，1-正常，2-锁定 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.10 用户兼职表 (sys_user_post)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| dept_id | BIGINT | 兼职部门ID |
| post_id | BIGINT | 兼职岗位ID |
| is_main | TINYINT | 是否主部门：0-兼职，1-主部门 |

#### 2.1.11 角色表 (sys_role)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| role_key | VARCHAR(64) | 角色Key |
| role_name | VARCHAR(64) | 角色名称 |
| role_type | TINYINT | 角色类型：1-系统角色，2-业务角色 |
| parent_id | BIGINT | 父角色ID |
| sort_order | INT | 排序 |
| status | TINYINT | 状态：0-停用，1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.12 用户角色关联表 (sys_user_role)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| role_id | BIGINT | 角色ID |

#### 2.1.13 权限表 (sys_permission)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| parent_id | BIGINT | 父权限ID |
| perm_name | VARCHAR(64) | 权限名称 |
| perm_key | VARCHAR(64) | 权限Key |
| perm_type | TINYINT | 权限类型：1-菜单，2-按钮，3-操作 |
| resource_path | VARCHAR(128) | 资源路径 |
| sort_order | INT | 排序 |
| create_time | DATETIME | 创建时间 |

#### 2.1.14 角色权限关联表 (sys_role_permission)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| role_id | BIGINT | 角色ID |
| permission_id | BIGINT | 权限ID |

#### 2.1.15 数据权限表 (sys_data_permission)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| role_id | BIGINT | 角色ID |
| dept_id | BIGINT | 部门ID |
| data_scope | TININY | 数据范围：1-全部，2-部门，3-部门及子部门，4-仅本人 |

#### 2.1.16 访问日志表 (sys_access_log)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(64) | 用户名 |
| ip | VARCHAR(64) | IP地址 |
| user_agent | VARCHAR(512) | 用户代理 |
| url | VARCHAR(256) | 请求URL |
| method | VARCHAR(16) | 请求方法 |
| params | TEXT | 请求参数 |
| result | TINYINT | 结果：0-失败，1-成功 |
| error_msg | TEXT | 错误信息 |
| access_time | DATETIME | 访问时间 |

#### 2.1.17 操作日志表 (sys_operation_log)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| user_id | BIGINT | 用户ID |
| username | VARCHAR(64) | 用户名 |
| module | VARCHAR(64) | 模块 |
| operation | VARCHAR(64) | 操作类型 |
| method | VARCHAR(128) | 方法名 |
| params | TEXT | 请求参数 |
| result | TEXT | 返回结果 |
| before_data | TEXT | 修改前数据 |
| after_data | TEXT | 修改后数据 |
| ip | VARCHAR(64) | IP地址 |
| operation_time | DATETIME | 操作时间 |

#### 2.1.18 数据字典类型表 (sys_dict_type)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| dict_name | VARCHAR(64) | 字典名称 |
| dict_code | VARCHAR(64) | 字典编码 |
| dict_type | TINYINT | 字典类型：1-系统内置，2-业务自定义 |
| description | VARCHAR(256) | 描述 |
| status | TINYINT | 状态：0-停用，1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 2.1.19 数据字典项表 (sys_dict_item)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键ID |
| dict_type_id | BIGINT | 字典类型ID |
| item_text | VARCHAR(64) | 字典项文本 |
| item_value | VARCHAR(64) | 字典项值 |
| sort_order | INT | 排序 |
| status | TINYINT | 状态：0-停用，1-正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 2.2 ER关系图

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ ProcessDefinition│◄──────│ ProcessInstance │──────►│     Task        │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │                          │                         │
         │                          │                         │
         ▼                          ▼                         ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  FormDefinition │       │    Variable     │       │  TaskVariable   │
└─────────────────┘       └─────────────────┘       └─────────────────┘

┌─────────────────┐       ┌─────────────────┐
│   DataModel     │◄──────│ ModelInstance   │◄────── ProcessInstance
└─────────────────┘       └─────────────────┘

┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   SysUser    │◄──────│ SysUserRole │──────►│  SysRole    │
└──────┬───────┘       └──────────────┘       └──────┬───────┘
       │                                                  │
       │              ┌──────────────┐                   │
       └─────────────►│ SysUserPost │◄──────────────────┘
                      └──────┬───────┘
                             │
                      ┌──────┴───────┐
                      │   SysDept    │
                      └──────────────┘

┌──────────────┐       ┌──────────────┐
│  SysRole     │◄──────│SysRolePerm   │──────► SysPermission
└──────────────┘       └──────────────┘

┌────────────────┐       ┌─────────────────┐
│SysAccessLog   │       │SysOperationLog │
└────────────────┘       └─────────────────┘

┌────────────────┐       ┌────────────────┐
│  SysDictType  │◄──────│  SysDictItem  │
└────────────────┘       └────────────────┘
```

---

## 3. 接口设计

### 3.1 流程定义API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/process/definitions | GET | 获取流程定义列表 |
| /api/v1/process/definitions/{id} | GET | 获取流程定义详情 |
| /api/v1/process/definitions | POST | 创建流程定义 |
| /api/v1/process/definitions/{id} | PUT | 更新流程定义 |
| /api/v1/process/definitions/{id} | DELETE | 删除流程定义 |
| /api/v1/process/definitions/{id}/deploy | POST | 部署流程定义 |
| /api/v1/process/definitions/{id}/export | GET | 导出流程定义 |
| /api/v1/process/definitions/import | POST | 导入流程定义 |

### 3.2 流程实例API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/process/instances | GET | 获取流程实例列表 |
| /api/v1/process/instances/{id} | GET | 获取流程实例详情 |
| /api/v1/process/instances | POST | 发起流程实例 |
| /api/v1/process/instances/{id}/suspend | POST | 暂停流程实例 |
| /api/v1/process/instances/{id}/resume | POST | 恢复流程实例 |
| /api/v1/process/instances/{id}/terminate | POST | 终止流程实例 |
| /api/v1/process/instances/{id}/variables | GET | 获取流程变量 |
| /api/v1/process/instances/{id}/variables | PUT | 更新流程变量 |

### 3.3 任务API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/tasks/todo | GET | 获取待办任务 |
| /api/v1/tasks/done | GET | 获取已办任务 |
| /api/v1/tasks/{id} | GET | 获取任务详情 |
| /api/v1/tasks/{id}/claim | POST | 签收任务 |
| /api/v1/tasks/{id}/unclaim | POST | 取消签收 |
| /api/v1/tasks/{id}/complete | POST | 完成任务 |
| /api/v1/tasks/{id}/reject | POST | 驳回任务 |
| /api/v1/tasks/{id}/transfer | POST | 转办任务 |
| /api/v1/tasks/{id}/delegate | POST | 委派任务 |
| /api/v1/tasks/{id}/counter-sign/vote | POST | 会签投票 |
| /api/v1/tasks/{id}/add-sign | POST | 发起加签 |

### 3.4 表单API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/forms/{formKey} | GET | 获取表单定义 |
| /api/v1/forms/{formKey}/data | POST | 提交表单数据 |
| /api/v1/forms/instances/{processInstanceId}/data | GET | 获取流程表单数据 |
| /api/v1/tasks/{taskId}/form-permissions | GET | 获取任务表单权限 |

### 3.5 数据模型API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/data-models | GET | 获取数据模型列表 |
| /api/v1/data-models/{modelKey} | GET | 获取数据模型详情 |
| /api/v1/data-models | POST | 创建数据模型 |
| /api/v1/data-models/{modelKey} | PUT | 更新数据模型 |
| /api/v1/data-models/{modelKey} | DELETE | 删除数据模型 |
| /api/v1/data-models/{modelKey}/publish | POST | 发布数据模型 |
| /api/v1/data-models/{modelKey}/instances | POST | 创建模型实例 |
| /api/v1/data-models/instances/{instanceId} | GET | 获取模型实例数据 |
| /api/v1/data-models/instances/{instanceId} | PUT | 更新模型实例数据 |

### 3.6 回调Webhook API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/webhooks/{webhookId}/trigger | POST | 触发Webhook回调 |
| /api/v1/webhooks/logs | GET | 获取回调日志 |

### 3.7 部门管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/depts | GET | 获取部门列表 |
| /api/v1/system/depts/{id} | GET | 获取部门详情 |
| /api/v1/system/depts | POST | 创建部门 |
| /api/v1/system/depts/{id} | PUT | 更新部门 |
| /api/v1/system/depts/{id} | DELETE | 删除部门 |
| /api/v1/system/depts/{id}/leader | PUT | 设置部门领导 |

### 3.8 用户管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/users | GET | 获取用户列表 |
| /api/v1/system/users/{id} | GET | 获取用户详情 |
| /api/v1/system/users | POST | 创建用户 |
| /api/v1/system/users/{id} | PUT | 更新用户 |
| /api/v1/system/users/{id} | DELETE | 删除用户 |
| /api/v1/system/users/{id}/reset-pwd | POST | 重置密码 |
| /api/v1/system/users/{id}/posts | GET | 获取用户兼职信息 |
| /api/v1/system/users/{id}/posts | POST | 添加兼职 |
| /api/v1/system/users/{id}/posts/{postId} | DELETE | 删除兼职 |

### 3.9 角色管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/roles | GET | 获取角色列表 |
| /api/v1/system/roles/{id} | GET | 获取角色详情 |
| /api/v1/system/roles | POST | 创建角色 |
| /api/v1/system/roles/{id} | PUT | 更新角色 |
| /api/v1/system/roles/{id} | DELETE | 删除角色 |
| /api/v1/system/roles/{id}/users | GET | 获取角色关联用户 |
| /api/v1/system/roles/{id}/users | POST | 分配用户 |

### 3.10 权限管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/permissions | GET | 获取权限列表 |
| /api/v1/system/permissions | POST | 创建权限 |
| /api/v1/system/permissions/{id} | PUT | 更新权限 |
| /api/v1/system/permissions/{id} | DELETE | 删除权限 |
| /api/v1/system/roles/{id}/permissions | GET | 获取角色权限 |
| /api/v1/system/roles/{id}/permissions | PUT | 分配权限 |
| /api/v1/system/roles/{id}/data-scope | PUT | 设置数据权限 |

### 3.11 日志管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/logs/access | GET | 获取访问日志 |
| /api/v1/system/logs/access/export | GET | 导出访问日志 |
| /api/v1/system/logs/operation | GET | 获取操作日志 |
| /api/v1/system/logs/operation/export | GET | 导出操作日志 |

### 3.12 数据字典API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/dict/types | GET | 获取字典类型列表 |
| /api/v1/system/dict/types/{id} | GET | 获取字典类型详情 |
| /api/v1/system/dict/types | POST | 创建字典类型 |
| /api/v1/system/dict/types/{id} | PUT | 更新字典类型 |
| /api/v1/system/dict/types/{id} | DELETE | 删除字典类型 |
| /api/v1/system/dict/items | GET | 获取字典项列表 |
| /api/v1/system/dict/items/{typeId} | GET | 获取字典项（按类型） |
| /api/v1/system/dict/items | POST | 创建字典项 |
| /api/v1/system/dict/items/{id} | PUT | 更新字典项 |
| /api/v1/system/dict/items/{id} | DELETE | 删除字典项 |

### 3.13 三员管理API

| 接口路径 | 方法 | 说明 |
|----------|------|------|
| /api/v1/system/admin/types | GET | 获取管理员类型 |
| /api/v1/system/admin/users | GET | 获取三员用户列表 |
| /api/v1/system/admin/audit-logs | GET | 获取三员操作审计日志 |

---

## 4. 核心模块详细设计

### 4.1 流程引擎核心设计

#### 4.1.1 流程执行器 (FlowEngine)

```java
public class FlowEngine {
    
    /**
     * 发起流程实例
     */
    public ProcessInstance startProcess(StartProcessRequest request);
    
    /**
     * 执行节点
     */
    public void executeNode(ExecutionContext context);
    
    /**
     * 处理任务完成
     */
    public void completeTask(CompleteTaskRequest request);
    
    /**
     * 回退流程
     */
    public void rollback(ExecutionContext context, String targetNodeId);
}
```

#### 4.1.2 执行上下文 (ExecutionContext)

```java
public class ExecutionContext {
    private String processInstanceId;
    private String processDefinitionId;
    private String currentNodeId;
    private Map<String, Object> variables;
    private String operator;
    
    // 获取变量
    public Object getVariable(String key);
    public Object getVariable(String key, Object defaultValue);
    
    // 设置变量
    public void setVariable(String key, Object value);
}
```

#### 4.1.3 流程JSON解析器

```java
public class ProcessJsonParser {
    
    /**
     * 解析流程定义JSON
     */
    public ProcessDefinition parse(String processJson);
    
    /**
     * 获取下一个节点
     */
    public Node getNextNode(ProcessDefinition def, String currentNodeId, Map<String, Object> variables);
    
    /**
     * 判断连线条件
     */
    public boolean evaluateCondition(String condition, Map<String, Object> variables);
}
```

### 4.2 任务中心设计

#### 4.2.1 会签处理器

```java
public class CounterSignHandler {
    
    /**
     * 创建会签任务
     */
    public void createCounterSignTasks(Node node, ExecutionContext context);
    
    /**
     * 处理会签投票
     */
    public void submitVote(CounterSignVoteRequest request);
    
    /**
     * 计算会签结果
     */
    public CounterSignResult calculateResult(String taskId);
}
```

#### 4.2.2 加签处理器

```java
public class AddSignHandler {
    
    /**
     * 发起加签
     */
    public void initiateAddSign(AddSignRequest request);
    
    /**
     * 处理加签审批
     */
    public void approveAddSign(AddSignApproveRequest request);
    
    /**
     * 返回原审批人
     */
    public void returnToOriginal(AddSignRequest request);
}
```

### 4.3 数据模型设计

#### 4.3.1 数据模型解析器

```java
public class DataModelParser {
    
    /**
     * 解析数据模型JSON
     */
    public DataModel parse(String modelJson);
    
    /**
     * 校验数据模型
     */
    public ValidationResult validate(DataModel model, Object data);
    
    /**
     * 计算计算字段
     */
    public Object computeField(ComputedField field, Map<String, Object> data);
}
```

#### 4.3.2 数据实例管理器

```java
public class ModelInstanceManager {
    
    /**
     * 创建模型实例
     */
    public String createInstance(String modelKey, Map<String, Object> data);
    
    /**
     * 更新模型实例
     */
    public void updateInstance(String instanceId, Map<String, Object> data);
    
    /**
     * 获取模型实例
     */
    public ModelInstance getInstance(String instanceId);
    
    /**
     * 校验数据
     */
    public ValidationResult validate(String modelKey, Map<String, Object> data);
}
```

### 4.4 节点回调设计

#### 4.4.1 Webhook调度器

```java
public class WebhookScheduler {
    
    /**
     * 触发节点回调
     */
    public void triggerWebhook(WebhookConfig config, Map<String, Object> payload);
    
    /**
     * 重试回调
     */
    public void retryWebhook(Long webhookLogId);
    
    /**
     * 调度异步回调
     */
    public void scheduleAsyncWebhook(WebhookConfig config, Map<String, Object> payload);
}
```

#### 4.4.2 回调配置结构

```java
public class WebhookConfig {
    private String url;                    // 回调地址
    private HttpMethod method;             // 请求方法
    private Map<String, String> headers;   // 请求头
    private String payloadTemplate;        // 载荷模板
    private int timeout;                   // 超时时间(毫秒)
    private int retryCount;                // 重试次数
    private List<String> triggerEvents;    // 触发事件列表
}
```

### 4.5 表单权限设计

#### 4.5.1 权限计算器

```java
public class PermissionCalculator {
    
    /**
     * 计算任务表单权限
     */
    public FormPermissions calculatePermissions(Task task, String formKey);
    
    /**
     * 合并字段权限
     */
    public List<FieldPermission> mergePermissions(
        List<FieldPermission> inherited, 
        List<FieldPermission> overrides
    );
    
    /**
     * 评估条件化权限
     */
    public FieldPermission evaluateCondition(
        FieldPermission permission, 
        Map<String, Object> variables
    );
}
```

---

## 5. 关键技术设计

### 5.1 流程状态机设计

```
┌─────────┐     发起     ┌─────────┐    完成     ┌─────────┐
│  草稿   │ ──────────▶ │ 运行中   │ ──────────▶ │ 已完成  │
└─────────┘              └─────────┘              └─────────┘
                         │      ▲
                         │      │
                      暂停/终止 │
                         │      │
                         ▼      │
                       ┌─────────┐    恢复     ┌─────────┐
                       │ 已暂停  │ ◀───────── │  已终止  │
                       └─────────┘            └─────────┘
```

### 5.2 分布式锁设计

使用本地锁+数据库乐观锁，保证流程实例并发安全：

```java
@Lock annotation
public class DistributedLock {
    
    /**
     * 获取锁
     */
    public boolean tryLock(String key, String requestId, long expireTime);
    
    /**
     * 释放锁
     */
    public void unlock(String key, String requestId);
}
```

### 5.3 事件驱动设计

使用Spring Event实现流程事件分发（单节点内）或通过定时任务轮询（多节点）：

| 事件类型 | 说明 | 消费者 |
|---------|------|--------|
| PROCESS_STARTED | 流程开始 | 通知服务、监控服务 |
| NODE_ENTERED | 节点进入 | 任务服务 |
| TASK_COMPLETED | 任务完成 | 流程引擎 |
| PROCESS_COMPLETED | 流程完成 | 通知服务、归档服务 |
| WEBHOOK_TRIGGERED | 回调触发 | 回调服务 |

### 5.4 缓存设计

使用Caffeine本地缓存框架：

| 缓存Key | 缓存内容 | 过期时间 | 最大条目 |
|--------|---------|----------|---------|
| process:def:{key}:{version} | 流程定义JSON | 24小时 | 1000 |
| process:instance:{id} | 流程实例 | 实时 | 5000 |
| task:todo:{userId} | 用户待办任务列表 | 5分钟 | 10000 |
| form:def:{formKey} | 表单定义 | 24小时 | 500 |
| model:def:{modelKey} | 数据模型定义 | 24小时 | 500 |

### 5.5 后台管理模块设计

#### 5.5.1 组织架构管理器

```java
public class DeptService {

    /**
     * 获取部门树形列表
     */
    public List<DeptTree> getDeptTree();

    /**
     * 创建部门
     */
    public void createDept(Dept dept);

    /**
     * 更新部门
     */
    public void updateDept(Dept dept);

    /**
     * 删除部门（转移用户）
     */
    public void deleteDept(Long id, Long targetDeptId);

    /**
     * 设置部门领导
     */
    public void setLeader(Long deptId, Long leaderId);
}
```

#### 5.5.2 用户管理器

```java
public class UserService {

    /**
     * 创建用户
     */
    public void createUser(User user);

    /**
     * 更新用户
     */
    public void updateUser(User user);

    /**
     * 设置用户密级
     */
    public void setSecurityLevel(Long userId, Integer securityLevel);

    /**
     * 添加用户兼职
     */
    public void addUserPost(Long userId, Long deptId, Long postId, boolean isMain);

    /**
     * 获取用户所有部门（主部门+兼职）
     */
    public List<UserDept> getUserDepts(Long userId);

    /**
     * 获取用户可访问的部门列表（含兼职）
     */
    public List<Long> getAccessibleDepts(Long userId);
}
```

#### 5.5.3 角色权限管理器

```java
public class RolePermissionService {

    /**
     * 创建角色
     */
    public void createRole(Role role);

    /**
     * 分配用户
     */
    public void assignUsers(Long roleId, List<Long> userIds);

    /**
     * 分配权限
     */
    public void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 设置数据权限
     */
    public void setDataScope(Long roleId, Long deptId, Integer dataScope);

    /**
     * 获取用户权限列表
     */
    public List<Permission> getUserPermissions(Long userId);

    /**
     * 获取用户数据权限范围
     */
    public DataScope getUserDataScope(Long userId);
}
```

#### 5.5.4 日志管理器

```java
public class LogService {

    /**
     * 记录访问日志
     */
    public void recordAccessLog(AccessLog log);

    /**
     * 记录操作日志
     */
    public void recordOperationLog(OperationLog log);

    /**
     * 查询访问日志
     */
    public PageResult<AccessLog> queryAccessLog(AccessLogQuery query);

    /**
     * 查询操作日志
     */
    public PageResult<OperationLog> queryOperationLog(OperationLogQuery query);

    /**
     * 清理过期日志
     */
    public void cleanExpiredLogs(int retentionDays);
}
```

#### 5.5.5 权限决策器

```java
public class PermissionEvaluator {

    /**
     * 判断用户是否有权限
     */
    public boolean hasPermission(Long userId, String permKey);

    /**
     * 判断用户是否可以访问数据
     */
    public boolean canAccessData(Long userId, Long deptId, String dataType);

    /**
     * 获取用户可访问的部门列表
     */
    public List<Long> getAccessibleDepts(Long userId);

    /**
     * 校验用户密级
     */
    public boolean checkSecurityLevel(Long userId, Integer requiredLevel);
}
```

#### 5.5.6 数据字典管理器

```java
public class DictService {

    /**
     * 获取字典类型列表
     */
    public List<DictType> getDictTypes();

    /**
     * 创建字典类型
     */
    public void createDictType(DictType dictType);

    /**
     * 更新字典类型
     */
    public void updateDictType(DictType dictType);

    /**
     * 删除字典类型（系统内置不可删除）
     */
    public void deleteDictType(Long id);

    /**
     * 获取字典项列表
     */
    public List<DictItem> getDictItems(Long typeId);

    /**
     * 根据字典编码获取字典项
     */
    public List<DictItem> getDictItemsByCode(String dictCode);

    /**
     * 创建字典项
     */
    public void createDictItem(DictItem dictItem);

    /**
     * 更新字典项
     */
    public void updateDictItem(DictItem dictItem);

    /**
     * 删除字典项
     */
    public void deleteDictItem(Long id);
}
```

#### 5.5.7 三员管理器

```java
public class TripleAdminService {

    /**
     * 获取管理员类型
     * 返回：系统管理员、安全管理员、审计管理员
     */
    public List<AdminType> getAdminTypes();

    /**
     * 判断用户是否为系统管理员
     */
    public boolean isSystemAdmin(Long userId);

    /**
     * 判断用户是否为安全管理员
     */
    public boolean isSecurityAdmin(Long userId);

    /**
     * 判断用户是否为审计管理员
     */
    public boolean isAuditAdmin(Long userId);

    /**
     * 校验操作权限（三员之间不能互相操作）
     */
    public void validateTripleAdminOperation(Long operatorId, Long targetId);

    /**
     * 获取三员操作审计日志
     */
    public PageResult<OperationLog> getTripleAdminAuditLogs(TripleAdminQuery query);

    /**
     * 初始化三员账号
     */
    public void initTripleAdmins();
}
```

#### 5.5.8 三员权限决策器

```java
public class TripleAdminPermissionEvaluator {

    /**
     * 系统管理员权限校验
     * - 可管理普通用户
     * - 可进行系统配置
     * - 不可管理安全员、审计员
     */
    public boolean canManageUser(Long operatorId, Long targetId);

    /**
     * 安全管理员权限校验
     * - 可分配角色权限
     * - 可管理安全策略
     * - 不可管理系统管理员、审计员
     */
    public boolean canManageSecurity(Long operatorId, Long targetId);

    /**
     * 审计管理员权限校验
     * - 可查看所有日志
     * - 不可进行业务操作
     * - 不可管理系统管理员、安全员
     */
    public boolean canManageAudit(Long operatorId, Long targetId);

    /**
     * 校验三员不能删除自己的账号
     */
    public boolean canDeleteSelf(Long operatorId);
}
```

---

## 6. 安全设计

### 6.1 认证授权

- **认证方式**：JWT Token + 内存会话
- **授权粒度**：功能权限 + 数据权限
- **接口鉴权**：Spring Security + 自定义过滤器

### 6.2 数据安全

- **敏感数据脱敏**：手机号、身份证号等字段脱敏
- **数据加密**：敏感配置加密存储
- **SQL注入防护**：MyBatis参数绑定

### 6.3 操作审计

- **日志记录**：操作日志、业务日志分离
- **审计字段**：操作人、操作时间、操作类型、操作前后数据

---

## 7. 部署架构

### 7.1 单节点部署

```
┌─────────────────────────────────────────────────────┐
│                     负载均衡                          │
│                      (Nginx)                        │
└───────────────────────┬─────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
   ┌─────────┐    ┌─────────┐    ┌─────────┐
   │ 节点1   │    │ 节点2   │    │ 节点3   │
   │(Spring  │    │(Spring  │    │(Spring  │
   │ Boot)   │    │ Boot)   │    │ Boot)   │
   └────┬────┘    └────┬────┘    └────┬────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
                        ▼
                   ┌─────────┐
                   │ SQLite  │
                   └─────────┘
```

### 7.2 Docker Compose配置

```yaml
version: '3.8'
services:
  flow-engine:
    image: flow-engine:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/data/flow_engine.db
    volumes:
      - ./data:/data
```

---

## 8. 接口详细规范

### 8.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1699999999999,
  "requestId": "xxx"
}
```

### 8.2 错误码规范

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 流程定义不存在 |
| 1002 | 流程实例不存在 |
| 1003 | 任务不存在 |
| 1004 | 表单不存在 |
| 1005 | 数据模型不存在 |

---

**文档结束**
