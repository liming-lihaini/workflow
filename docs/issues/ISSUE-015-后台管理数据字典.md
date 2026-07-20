# ISSUE-015：后台管理 — 数据字典

- **优先级**：P0
- **状态**：✅ 已完成（2026-07-15）
- **模块**：后台管理 / 字典
- **负责人**：后端×2
- **预计工期**：W10
- **前置依赖**：ISSUE-001
- **里程碑**：M4

## 目标
提供数据字典类型与字典项维护，支持系统内置字典（不可删除）与业务自定义字典，供表单/流程配置复用。

## 范围
### In Scope
- 表：`sys_dict_type`（§2.1.18）、`sys_dict_item`（§2.1.19）。
- 服务：`DictService` 全套（§5.5.6）：类型/项 CRUD、按编码取项。
- API：`/api/v1/system/dict/types*`、`/dict/items*`（TRD §3.12）。
- 约束：系统内置字典不可删除（PRD §数据字典管理）。

### Out of Scope
- 字典在表单/流程中的具体引用（由各业务 Issue 使用）。

## 技术约束（以 TRD 为准）
- 表 §2.1.18/19；API §3.12；服务 §5.5.6。

## 接口契约
- REST：`/api/v1/system/dict/types`(GET/POST)、`/{id}`(GET/PUT/DELETE)、`/items`(GET/POST)、`/{id}`(PUT/DELETE)、`/items/{typeId}`(GET)。
- Java：`DictService`、`DictType`、`DictItem`。

## 独立运行与验证
### 运行方式
```bash
mvn spring-boot:run
# 可独立维护字典类型与项
```

### 验证用例（映射 PRD §后台管理 字典验收 / TRD §3.12/§5.5.6）
- [x] 创建字典类型 + 字典项；按类型/编码查询正确。
- [x] 删除业务自定义字典项成功；删除系统内置字典类型被拒（400）。
- [x] 更新/查询字典项生效。

### 验证脚本/测试
- `DictServiceTest`：CRUD + 内置不可删约束 + 按编码取项。

## 交付物
- 字典服务 + 接口 + 初始化系统内置字典数据。

## 验收门禁（DoD）
- [x] 字典类型/项可维护；系统内置不可删；按编码查询可用。

## 实现摘要
- 2 个实体：DictType、DictItem
- 2 个 Mapper：DictTypeMapper、DictItemMapper
- 1 个核心服务：DictService（类型/项 CRUD、按编码取项）
- 1 个 REST Controller：DictController（`/api/v1/system/dict/*`）
- 1 个数据初始化器：DictDataInitializer（应用启动时自动初始化系统内置字典）
- 4 个新增错误码：DICT_TYPE_NOT_FOUND(1039)、DICT_CODE_DUPLICATE(1040)、DICT_ITEM_NOT_FOUND(1041)、DICT_TYPE_BUILTIN(1042)

## 系统内置字典数据
- 流程状态（process_status）：运行中/已完成/已终止/已挂起
- 节点类型（node_type）：开始/结束/用户任务/服务任务/脚本任务/排他网关/并行网关/包容网关/子流程/会签节点
- 任务状态（task_status）：待处理/已完成/已驳回/已撤回/已转办/已委托
- 会签模式（counter_sign_mode）：一票通过/全票通过/比例通过/票数通过
- 加签类型（sign_type）：前加签/后加签/并行加签
- 数据权限范围（data_scope）：全部/本部门/本部门及子部门/仅本人
- 密级（security_level）：公开/内部/秘密/机密
- 部门类型（dept_type）：公司/部门/小组
- 字典类型分类（dict_type_category）：系统内置/业务自定义

## 测试覆盖
- DictServiceTest（12 用例）
- 全部 179 个测试通过，BUILD SUCCESS
