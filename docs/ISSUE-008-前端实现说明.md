# ISSUE-008 表单引擎 - 前端实现说明

## 概述

本文档说明 ISSUE-008 表单引擎的前后端实现，包含：
1. 表单定义管理
2. 可视化表单设计器
3. 数据模型对接

## 技术栈

- **后端**：Spring Boot 3.x + MyBatis Plus + SQLite
- **前端**：Vue 3.5 + Ant Design Vue 4.x + Vite 5.x

---

## 一、后端实现

### 1.1 数据库表

已存在的表结构（`schema.sql`）：

```sql
-- 表单定义表
CREATE TABLE IF NOT EXISTS wf_form_definition (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    form_key    TEXT,
    form_name   TEXT,
    form_json   TEXT,
    category    TEXT,
    model_key   TEXT,
    create_time TEXT,
    update_time TEXT
);

-- 唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_form_definition_form_key ON wf_form_definition(form_key);
```

### 1.2 新增 Java 类

| 类 | 路径 | 说明 |
|---|---|---|
| FormDefinition | `entity/FormDefinition.java` | 表单定义实体 |
| FormDefinitionMapper | `mapper/FormDefinitionMapper.java` | MyBatis Plus Mapper |
| FormDefinitionService | `service/FormDefinitionService.java` | 表单业务服务 |
| FormController | `controller/FormController.java` | REST API 控制器 |

### 1.3 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/forms | 获取表单列表（分页） |
| GET | /api/v1/forms/all | 获取所有表单（下拉选择用） |
| GET | /api/v1/forms/{formKey} | 获取表单详情 |
| POST | /api/v1/forms | 创建表单 |
| PUT | /api/v1/forms/{formKey} | 更新表单 |
| DELETE | /api/v1/forms/{formKey} | 删除表单 |

### 1.4 错误码

| 错误码 | 说明 |
|--------|------|
| 1051 | FORM_KEY_DUPLICATE - 表单Key已存在 |
| 1052 | FORM_NOT_FOUND - 表单不存在 |
| 1053 | FORM_VALIDATION_FAILED - 表单校验失败 |

---

## 二、前端实现

### 2.1 新增 API 模块

**`src/api/form.js`**
```javascript
export function getFormList(params)
export function getFormAll()
export function getForm(formKey)
export function createForm(data)
export function updateForm(formKey, data)
export function deleteForm(formKey)
```

**`src/api/model.js`**
```javascript
export function getDataModelList(params)
export function getDataModel(modelKey)
export function createDataModel(data)
export function updateDataModel(modelKey, data)
export function deleteDataModel(modelKey)
export function publishDataModel(modelKey)
export function getDataModelFormFields(modelKey)
```

### 2.2 新增页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 表单定义 | `views/form/definition/index.vue` | 表单列表、新建、编辑、删除 |
| 表单设计器 | `views/form/design/index.vue` | 可视化拖拽设计 |
| 数据模型 | `views/data-model/index.vue` | 数据模型列表、发布 |

### 2.3 路由配置

```javascript
// router/index.js
{ path: 'form/definition', name: 'FormDefinition', meta: { title: '表单定义' } }
{ path: 'form/design', name: 'FormDesign', meta: { title: '表单设计器' } }
{ path: 'data-model', name: 'DataModel', meta: { title: '数据模型' } }
```

### 2.4 菜单配置

```
流程管理
├── 流程定义
├── 流程设计器
├── 流程实例
├── 表单定义      ← 新增
├── 表单设计器    ← 新增
└── 数据模型      ← 新增
```

---

## 三、表单设计器

### 3.1 功能特性

- **左侧组件面板**：拖拽组件到画布
- **中间画布**：实时预览表单
- **右侧属性配置**：配置字段属性

### 3.2 支持的组件

| 组件类型 | 字段标识 | 说明 |
|----------|----------|------|
| 单行文本 | text | 文本输入 |
| 多行文本 | textarea | 多行文本 |
| 数字 | number | 数字输入 |
| 金额 | amount | 金额输入 |
| 日期 | date | 日期选择 |
| 时间 | time | 时间选择 |
| 日期时间 | datetime | 日期时间选择 |
| 下拉选择 | select | 下拉单选 |
| 单选框 | radio | 单选 |
| 复选框 | checkbox | 多选 |
| 文件上传 | file | 文件上传 |
| 人员选择 | user | 人员选择器 |
| 部门选择 | dept | 部门选择器 |

### 3.3 字段属性

| 属性 | 说明 |
|------|------|
| field | 字段标识（唯一） |
| label | 字段标签 |
| placeholder | 占位符 |
| required | 是否必填 |
| defaultValue | 默认值 |
| optionsText | 选项配置（下拉/单选/复选用） |

### 3.4 表单 JSON 结构

```json
[
  {
    "id": "field_1",
    "type": "text",
    "field": "title",
    "label": "标题",
    "placeholder": "请输入标题",
    "required": true,
    "defaultValue": ""
  },
  {
    "id": "field_2",
    "type": "select",
    "field": "category",
    "label": "分类",
    "placeholder": "请选择分类",
    "required": true,
    "optionsText": "1:技术\n2:产品\n3:运营"
  }
]
```

---

## 四、数据模型

### 4.1 功能说明

- 创建/编辑数据模型
- 发布模型（草稿 → 已发布）
- 删除草稿状态的模型

### 4.2 字段定义

| 属性 | 说明 |
|------|------|
| fieldKey | 字段标识 |
| fieldName | 字段名称 |
| fieldType | 字段类型：text/number/amount/date/datetime/file/person/department |
| required | 是否必填 |
| validationRules | 校验规则 |

### 4.3 主子表关系

数据模型支持：
- 主表（1个）
- 子表（0~10个）
- 字段数量限制：每个子表最多50个字段

---

## 四、表单与数据模型绑定

### 4.1 功能说明

在表单设计器中可以绑定数据模型，选择模型后自动生成表单字段。

### 4.2 绑定流程

1. 进入表单设计器页面
2. 在顶部「绑定数据模型」下拉框中选择已发布的数据模型
3. 系统自动从数据模型生成表单字段（主表 + 第一个子表）
4. 字段标记为「模型」标签，标识来源
5. 点击「保存」同时保存表单配置和模型绑定关系

### 4.3 字段映射规则

| 数据模型字段类型 | 表单组件类型 |
|----------------|-------------|
| text | 单行文本 |
| number | 数字 |
| amount | 金额 |
| date | 日期 |
| datetime | 日期时间 |
| file | 文件上传 |
| person | 人员选择 |
| department | 部门选择 |

### 4.4 绑定信息存储

```json
{
  "id": "field_1",
  "type": "text",
  "field": "title",
  "label": "标题",
  "fromModel": true,
  "modelKey": "leave-data",
  "tableName": "main_table"
}
```

- `fromModel`: 标识字段是否来自数据模型
- `modelKey`: 绑定的数据模型Key
- `tableName`: 所属表名（主表或子表）

### 4.5 使用约束

- 只能绑定「已发布」状态的数据模型
- 重新选择模型会覆盖现有字段
- 手动拖拽添加的字段不受模型影响

---

## 五、使用流程

### 5.1 创建表单

1. 进入「表单定义」页面
2. 点击「新建」，填写表单Key、名称、分类
3. 点击「设计」进入表单设计器

### 5.2 设计表单

1. 从左侧拖拽组件到中间画布
2. 点击画布中的字段，右侧显示属性配置
3. 配置字段标签、占位符、必填等属性
4. 点击「保存」保存表单

### 5.3 创建数据模型

1. 进入「数据模型」页面
2. 点击「新建」，填写模型Key、名称
3. 点击「发布」发布模型

---

## 六、文件清单

### 后端
```
flow-engine/src/main/java/com/flow/engine/
├── entity/FormDefinition.java
├── mapper/FormDefinitionMapper.java
├── service/FormDefinitionService.java
└── controller/FormController.java
```

### 前端
```
flow-web/src/
├── api/
│   ├── form.js
│   └── model.js
├── views/
│   ├── form/
│   │   ├── definition/index.vue
│   │   └── design/index.vue
│   └── data-model/
│       └── index.vue
├── router/index.js
└── layouts/BasicLayout.vue
```

---

## 七、后续优化

- [ ] 表单设计器支持更多布局（栅格、分栏）
- [ ] 表单设计器支持自定义校验规则
- [ ] 表单设计器支持数据绑定（绑定数据模型）
- [ ] 表单预览功能
- [ ] 表单模板功能

---

## 八、相关文档

- [ISSUE-008-表单引擎.md](../issues/ISSUE-008-表单引擎.md)
- [01_自定义流程引擎_PRD.md](../prd/01_自定义流程引擎_PRD.md)
- [01_自定义流程引擎_TRD.md](../trd/01_自定义流程引擎_TRD.md)
