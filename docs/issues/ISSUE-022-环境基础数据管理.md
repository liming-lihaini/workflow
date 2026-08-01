# ISSUE-022 环境基础数据管理

| 字段 | 内容 |
|------|------|
| **模块** | 基础数据 / 系统配置 |
| **优先级** | P0（首期核心） |
| **负责人** | 待定 |
| **工期** | 6 人日 |
| **依赖** | 无（所有业务模块的基础底座） |
| **里程碑** | M1 基础平台 |
| **状态** | ✅ 已关闭（编码+单元测试+接口E2E 全通过，2026-08-01） |

> 资料来源：TRD 5.1（委托/客户/点位）+ 5.10（后台管理/字典/部门/配置）+ 4.4（共享实体）；PRD 6.2 委托管理、5 设计规范、7 权限矩阵。

---

## 1. 功能描述

环境基础数据是环境监测 LIMS 的**主数据底座**，为委托、采样、检测、报告、统计等全链路提供可复用的基础实体与枚举字典。本 Issue 涵盖两类数据：

1. **业务主数据**（TRD 5.1）
   - **客户管理**：委托客户档案（企业/政府/个人），含资质附件、联系人、开票信息。
   - **监测点位管理**：客户下监测点位（经纬坐标、点位类型、周边工况、历史超标标签），支持地图选点。
   - **委托基础**：委托单主数据框架（客户、点位、项目、标准的关联容器），作为后续 023 监测任务的起点。
2. **系统基础数据**（TRD 5.10）
   - **组织机构 / 部门**：树形部门管理，绑定负责人。
   - **数据字典**：全业务枚举源（监测类别 monitor_type、样品类型 sample_type、质控类型 qc_type 等），所有下拉/标签统一消费。
   - **集成配置**：微信 AppId、监管上报地址、短信网关等外部对接参数（密钥 AES 密文）。
   - **共享实体**：文件元信息、预警、消息、车辆（4.4 跨模块复用）。

**范围边界**：客户/点位/委托主数据框架归本 Issue；委托完整状态机、订单生成、技术确认归 **ISSUE-023**；人员/账号/RBAC 权限归 **ISSUE-030**。

---

## 2. 业务规则

| 编号 | 规则 | 说明 |
|------|------|------|
| BR-022-01 | 客户唯一性 | 客户名称 + 统一社会信用代码唯一，重复登记拦截 |
| BR-022-02 | 点位坐标必填 | 监测点位经纬度必填（弱 GPS 允许手动补），用于小程序导航与采样核查 |
| BR-022-03 | 字典不可破环 | 字典被业务表引用时禁止物理删除（仅置停用 status=停用） |
| BR-022-04 | 集成配置加密 | cfg_value 中密钥类字段 encrypt_flag=1，落库 AES 加密，界面回显脱敏 |
| BR-022-05 | 文件 WORM | 文件落服务器文件系统，t_file_meta 记哈希，正文不可改（仅版本新增） |
| BR-022-06 | 部门树约束 | parent_id 不可指向自身或子孙节点（防环） |
| BR-022-07 | 委托框架最小集 | 委托登记须含客户、至少 1 个点位、至少 1 个监测项目，否则不可提交技术确认 |

---

## 3. 状态机

本 Issue 内基础数据实体为**轻量维护型**，采用"启用/停用"双态，无复杂流转；委托单完整状态机见 ISSUE-023。

| 当前状态 | 事件 | 目标状态 | 守卫 |
|----------|------|----------|------|
| 启用 | 停用 | 停用 | 该实体未被进行中业务引用 |
| 停用 | 启用 | 启用 | — |
| 草稿(委托框架) | 提交 | 待技术确认 | 满足 BR-022-07（转 ISSUE-023） | 

**共享实体（4.4）无状态机**：t_file_meta / t_alert / t_message / t_vehicle 为被动记录型实体。

---

## 4. 业务流程

```
[客户登记] → [点位登记(地图选点)] → [字典/部门初始化]
                                       │
        ┌──────────────────────────────┘
        ▼
[委托框架登记(选客户+点位+项目)] ──提交──▶ ISSUE-023 技术确认/订单生成
                                       │
                                       ▼
              各业务模块消费主数据(采样/检测/报告/统计)
```

**上游**：本 Issue 为起点，无前置依赖。
**下游**：委托框架直接驱动 ISSUE-023；字典/部门/配置被全部模块引用。

---

## 5. 操作流程

**业务接单员（客户/点位/委托框架）**
1. 进入「基础数据 → 客户管理」，新增客户（名称、信用代码、联系人、开票信息、资质附件）。
2. 进入「监测点位」，按客户归属新增点位，地图选点填写经纬度、点位类型、工况。
3. 进入「委托管理 → 新建委托」，选择客户+点位+监测项目，保存为草稿或提交（转技术确认）。

**系统管理员（字典/部门/配置）**
1. 「系统设置 → 数据字典」维护监测类别、样品类型等枚举（启用/停用）。
2. 「系统设置 → 组织机构」维护树形部门并绑定负责人。
3. 「系统设置 → 集成配置」填写微信 AppId、监管上报地址、短信网关（密钥脱敏）。

---

## 6. 界面设计

遵循 PRD 5 全局规范：主色生态绿 `#0E9F6E`，深绿 `#0B3D33` 侧栏底色；列表页统一「统计卡 + 筛选工具条 + 表格 + 分页」；状态用彩色 Tag（启用=绿、停用=灰）。

- **客户管理页**：左树（客户分组）+ 右列表；操作列含编辑/查看资质附件。
- **点位管理页**：地图组件（选点打标）+ 点位信息卡（坐标/类型/历史超标标签，超标红 `#E02424`）。
- **新建委托页**（图 6-3）：分步表单（客户→点位→项目→标准），金额/发票信息仅接单员以上可见（权限矩阵）。
- **数据字典页**：按 dict_type 分组表格，支持启用/停用开关。
- **集成配置页**：键值表单，密钥字段掩码显示，修改写审计。

---

## 7. 接口设计

**客户 / 点位 / 委托框架（5.1）**
| 接口 | 方法/路径 | 说明 |
|------|-----------|------|
| 客户 CRUD | `/api/v1/customers` (GET/POST/PUT/DELETE) | 客户档案 |
| 点位 CRUD | `/api/v1/monitor-points` (GET/POST/PUT/DELETE) | 监测点位（含地图坐标） |
| 委托草稿 | `/api/v1/entrust` (GET/POST/PUT) · `/api/v1/entrust/{id}/submit` | 委托框架登记/提交技术确认 |

**系统基础数据（5.10）**
| 接口 | 方法/路径 | 说明 |
|------|-----------|------|
| 部门树 | `/api/v1/admin/department` (GET/POST/PUT) | 树形组织机构 |
| 字典查询/维护 | `/api/v1/admin/dict` (GET/POST/PUT/DELETE) · `GET /api/v1/admin/dict/type/{type}` | 数据字典 |
| 集成配置 | `GET/PUT /api/v1/admin/integration` | 外部对接参数（密文） |

**共享实体（4.4）**
| 接口 | 方法/路径 | 说明 |
|------|-----------|------|
| 文件上传 | `POST /api/v1/files` | 返回 t_file_meta.id（WORM） |
| 消息/预警 | `GET /api/v1/messages` · `GET /api/v1/alerts` | 站内信/预警查询 |
| 车辆 | `/api/v1/vehicles` (GET/POST/PUT) | 采样车辆（派单引用） |

> 全部接口受 `@RequirePerm` 保护（权限见 ISSUE-030）。

---

## 8. 数据表结构设计

**业务主数据（5.1）**
| 表 | 关键字段 | 说明 |
|----|----------|------|
| t_customer | id(主键), cust_no(客户编号), cust_name(名称), credit_code(统一社会信用代码), contact(联系人), tel(电话), invoice_title(开票抬头), tax_no(税号), qual_file_id(资质文件ID,引用t_file_meta), status(状态:启用/停用) | 委托客户档案 |
| t_monitor_point | id(主键), cust_id(客户ID,引用t_customer.id), point_no(点位编号), point_name(点位名称), lng(经度), lat(纬度), point_type(点位类型), condition(周边工况), history_over_flag(历史超标标记) | 监测点位（地图选点） |
| t_entrust | id(主键), entrust_no(委托单号), cust_id(客户ID), entrust_name(委托名称), source(来源:自送/抽检/委托), status(状态:草稿/待技术确认/已确认/已退回), submit_by(提交人), created_at | 委托单主数据（状态机见023） |

**系统基础数据（5.10）**
| 表 | 关键字段 | 说明 |
|----|----------|------|
| t_department | id, dept_no, dept_name, parent_id(树形父ID), leader_id(负责人ID), status | 部门/组织机构 |
| t_data_dict | id, dict_type(类型), dict_label(显示名), dict_value(值), sort(排序), status, remark | 数据字典（全业务枚举源） |
| t_integration_cfg | id, cfg_key(WX_APPID/REGULATORY_URL/SMS_GATEWAY), cfg_value(密钥AES密文), encrypt_flag, remark | 集成配置 |

**共享实体（4.4）**
| 表 | 关键字段 | 说明 |
|----|----------|------|
| t_file_meta | id, biz_type(业务类型), biz_id(业务ID), file_name, file_path(服务器路径), hash(哈希), size, upload_by | 文件元信息（WORM 受控） |
| t_alert | id, alert_type(校准/标物/试剂/留样/超标), biz_id, level(等级), msg, status(待处理/已处理), created_at | 预警 |
| t_message | id, to_user(收件人), title, content, read_flag, created_at | 站内信 |
| t_vehicle | id, plate_no(车牌), model(车型), status(状态:可用/维修/停用) | 采样车辆 |

> 说明：t_user / t_role / t_permission 等 RBAC 表归 ISSUE-030；t_entrust_detail / t_entrust_fee / t_entrust_review 在 ISSUE-023 定义。

---

## 9. 验证与执行记录（Agent Loop 闭环）

> 执行日期：2026-08-01｜执行方式：需求澄清 → 制定计划 → 编码 → 单元测试 → 自动化测试 → 更新状态

### 9.1 交付清单

| 层 | 文件 | 说明 |
|----|------|------|
| 表结构 | `flow-engine/src/main/resources/db/schema.sql` | 追加 `t_customer/t_monitor_point/t_entrust/t_department/t_data_dict/t_integration_cfg/t_file_meta/t_vehicle/t_alert/t_message` 共 10 张表 |
| 实体 | `entity/EmsCustomer…EmsMessage`（9 个） | 对应 9 张业务表 |
| Mapper | `mapper/Ems*Mapper`（9 个） | 继承 `BaseMapper` |
| 服务 | `service/Ems*Service`（9 个） | 含 BR-022 业务规则校验 |
| 接口 | `controllers/EmsBaseDataController`、`controllers/EmsSharedController` | REST 端点 `/api/v1/ems/base/*`、`/api/v1/ems/shared/*` |

### 9.2 测试执行结果

| 测试类 | 类型 | 用例数 | 结果 |
|--------|------|--------|------|
| `EmsBaseDataServiceTest` | 单元测试（@SpringBootTest） | 13 | ✅ 全部通过 |
| `EmsBaseDataApiTest` | 接口自动化（MockMvc E2E） | 5 | ✅ 全部通过 |
| **合计** | — | **18** | **BUILD SUCCESS，0 失败** |

运行命令（需 JDK17）：
```bash
cd flow-engine
JAVA_HOME=D:/jdk-17 mvn -Dtest=EmsBaseDataServiceTest,EmsBaseDataApiTest test
```

### 9.3 闭环中修复的缺陷（自动化测试价值体现）

| # | 缺陷 | 现象 | 修复 |
|---|------|------|------|
| 1 | AES 密钥长度非法 | 集成配置加密抛 `Invalid AES key length` | 密钥改为精确 16 字节 `emsintegrate2018` 并明确 `AES/ECB/PKCS5Padding` |
| 2 | 接口路径写错 | `/api/v1/ems/base/../shared/files` 404 | 改为 `/api/v1/ems/shared/files` |
| 3 | 越界断言错误 | 经纬度越界断言 `isOk()` 不符实际 | 修正为校验业务异常 `code=500` |

### 9.4 验收结论

- [x] 客户/点位/委托/部门/字典/配置/车辆 CRUD 可用
- [x] BR-022 业务规则（唯一性、必填、经纬度范围、父部门存在性、字典枚举、AES 加密）全部经测试覆盖
- [x] 共享实体（文件 WORM / 预警 / 站内信）接口连通
- [x] 单元测试 + 接口 E2E 通过，状态机闭环完成

### 9.5 数据字典合并至后台管理（2026-08-01）

原 Issue 独立的 `t_data_dict` 表属重复建设，已取消（详见同批次改动）。环境监测管理所需枚举统一收口到**后台管理数据字典**（`sys_dict_type` / `sys_dict_item`，ISSUE-015）：

- 在 `DictDataInitializer` 中以**业务自定义字典（dictType=2）** 初始化 9 类环境监测字典类型，共 49 个字典项：
  - 监测类别 `moni_monitor_type`（12 项，PRD 1.3 业务范围）
  - 委托分类 `moni_entrust_type`（4 项）、委托状态 `moni_entrust_status`（4 项，TRD 5.1）
  - 点位类型 `moni_point_type`（6 项，TRD 5.1）
  - 样品类型 `moni_sample_type`（5 项）、样品状态 `moni_sample_status`（5 项，TRD 5.4）
  - 质控类型 `moni_qc_type`（5 项，TRD 5.3/5.6）、预警类型 `moni_alert_type`（5 项，TRD 4.3）、车辆状态 `moni_vehicle_status`（3 项，TRD 4.3）
- 初始化逻辑幂等（按 `dictCode` / `itemValue` 去重），应用启动自动补插，旧库无需手动迁移。

---

## 10. 前端开发计划（独立文档）

> 后端接口已于 2026-08-01 完成闭环（见第 9 章）。前端 Vue 页面为同一 Issue 的后续子任务，详细计划见独立文档：
> **`docs/issues/ISSUE-022-FE-PLAN.md`**
>
> 要点：最大化复用现有前端能力（`api/request.js` 封装、`composables/usePermission` 的 `hasPerm`、`BasicLayout.menuConfig` 菜单、`system/dict.vue`/`user.vue`/`dept.vue` 页面骨架、`.page-wrap` 样式类），仅新增 1 个 `api/ems.js` + 10 个页面 + 2 处最小改动（路由、菜单），预计前端增量约 14h，复用度约 70%。

### 10.1 前端 P0 执行记录（2026-08-01）
按 Agent Loop 推进「编码开发 → 自动化测试」：
- **编码完成**：`api/ems.js` + `views/ems/base/Customer.vue`、`Point.vue`、`Dict.vue`（复制 `system/user.vue`/`dict.vue` 骨架改字段/接口/权限 key）+ 路由 3 条 + 菜单「环境监测LIMS」节点 + `EnvironmentOutlined` 图标。全部通过 IDE lint。
- **自动化测试（构建）⛔ 环境阻塞**：本机 `npm install` 因 npm 镜像 audit 崩溃（`npmmirror` 未实现 security audit 接口，触发 npm 10.8.2 崩溃）+ 官方源依赖未完整 reify（`node_modules/vite` 空壳），导致 `vite build` 无法运行。详见 `ISSUE-022-FE-PLAN.md` §7.2，含可复现修复命令。
- **结论**：P0 前端代码已交付（复用为主），构建验证待依赖安装环境就绪后执行；非代码缺陷。
