# ISSUE-008：表单引擎 — 定义 / 数据绑定 / 校验

- **优先级**：P0
- **模块**：表单引擎
- **负责人**：后端×2
- **预计工期**：W6
- **前置依赖**：ISSUE-001
- **里程碑**：M2

## 目标
通过 JSON 定义表单结构（多组件类型），收集用户填写数据并与流程变量自动绑定，提供字段级校验（必填/格式/正则）。

## 范围
### In Scope
- 表单定义：`wf_form_definition`（TRD §2.1.4）；`GET /api/v1/forms/{formKey}`（TRD §3.4）。
- 组件库：单行/多行文本、数字、金额、日期、时间、日期时间、下拉、单选、多选、文件、人员、部门（PRD §表单定义）。
- 数据提交：`POST /api/v1/forms/{formKey}/data`；`GET /api/v1/forms/instances/{processInstanceId}/data`（TRD §3.4）。
- 数据绑定：表单字段 ↔ 流程变量自动映射；校验规则在定义中配置（PRD §表单数据绑定）。
- 约束：form_key 全局唯一（PRD §约束）。

### Out of Scope
- 表单权限（009）；数据模型绑定（010）。

## 技术约束（以 TRD 为准）
- 表：`wf_form_definition`（§2.1.4）；缓存 `form:def:{formKey}`（§5.4）。
- API `/api/v1/forms/*`（TRD §3.4）。

## 接口契约
- REST：`/api/v1/forms/{formKey}`(GET)、`/{formKey}/data`(POST)、`/instances/{pid}/data`(GET)。
- Java：`FormService.create / getForm / saveFormData / getFormData`；`FormDefinitionParser`（校验规则解析）。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 本 Issue 不依赖流程引擎，可独立 CRUD 表单并校验/提交数据
```

### 验证用例（映射 PRD §表单引擎 验收标准 / TRD §3.4）
- [ ] 定义含多组件类型的表单，`form_key` 唯一校验。
- [ ] 提交数据触发必填校验：缺失必填字段返回 400。
- [ ] 格式/正则校验生效（如金额非法被拒）。
- [ ] 提交后 `GET .../instances/{pid}/data` 取回一致数据。
- [ ] 表单字段与流程变量绑定映射正确（可与 004 联调时验证）。

### 验证脚本/测试
- `FormDefinitionApiTest`：CRUD + 唯一性。
- `FormValidationTest`：必填/格式/正则校验用例。

## 交付物
- 表单定义/数据服务 + 解析与校验 + 接口。

## 验收门禁（DoD）
- 可定义表单并收集数据；字段级校验（必填/格式/正则）生效；数据可持久化与查询。
