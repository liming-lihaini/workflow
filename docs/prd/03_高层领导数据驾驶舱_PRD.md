# 产品需求说明书（PRD）

## 封面

| **项目名称** | 高层领导数据驾驶舱 |
|---|---|
| **文档标题** | LIMS 高层领导数据驾驶舱产品需求说明书 |
| **编制日期** | 2026-08-14 |
| **编制人** | 产品团队 |

---

## 版本历史

| 版本 | 日期 | 变更说明 | 编制人 |
|------|------|---------|--------|
| V1.0 | 2026-08-14 | 初始版本，基于真实数据口径与现有系统表结构定义 | 产品团队 |

---

## 变更记录

> 如有变更则必填

| 变更日期 | 变更内容 | 变更人 |
|----------|---------|--------|
| - | - | - |

---

## 背景

### 需求背景

环境监测机构（LIMS）经营与质量数据分散在合同、检测、报告、仪器、预警等多个业务系统中。高层领导缺乏一个统一的、可实时刷新的经营与质量全景视图，无法在会议室大屏或办公电脑上快速掌握：

1. **经营态势**：合同总量、合同金额、回款进度、重点客户贡献、合同到期风险；
2. **业务流转**：从合同签订 → 检测任务 → 检测结果 → 报告生成的业务漏斗；
3. **质量与风险**：检测结果超标、系统预警、质控合格率；
4. **资产状态**：仪器设备台套数及在用/报废状态。

现有 `flow-web/src/views/dashboard/index.vue` 是**个人工作台**（待办、快捷入口、到期提醒），面向一线/中层操作员，维度和视角均不满足高层全局决策需求。需新增独立的**高层领导数据驾驶舱**页面。

### 需求发起人

高层经营管理层（总经理 / 经营副总 / 质量负责人）

### 需求预期效果

构建一个**深色科技风数据驾驶舱大屏**，数据全部来自业务库真实聚合（非模拟数据），支持：

1. 顶部 6 个核心 KPI 卡片；
2. 左-中-右三栏 11 个分析面板（趋势、分布、排名、漏斗、明细表、预警、仪表盘、到期列表）；
3. 底部实时滚动动态条；
4. 所有指标实时/准实时刷新（默认自动刷新），可手动刷新。

---

## 产品规划

### 产品定位、目标、愿景

**产品定位**：面向高层的经营与质量决策驾驶舱（只读聚合视图，跨 EMS 全子域）。

**产品目标**：
- 一屏掌握经营（合同/回款/客户）、业务（检测/报告）、质量（超标/预警/质控）、资产（仪器）四大维度；
- 数据 100% 来自真实业务表聚合，口径与现有系统一致；
- 支持大屏展示（≥1920×1080）与桌面浏览器查看。

**产品愿景**：成为机构经营决策的唯一全局数据入口。

### 产品目标受众

- **高层领导**：全局经营与质量态势；
- **经营/财务负责人**：回款率、合同到期、客户贡献；
- **质量负责人**：超标、预警、质控合格率。

---

## 产品框架

### 功能模块清单

| 编号 | 模块 | 说明 | 数据来源 |
|------|------|------|----------|
| C1 | KPI 指标卡 | 6 个核心指标 | 多表聚合 |
| C2 | 合同金额月度趋势 | 柱+线组合图 | `t_contract` |
| C3 | 合同结构分布 | 环形饼图（状态） | `t_contract` |
| C4 | 客户合同金额 TOP10 | 横向条形图 | `t_contract` |
| C5 | 业务全链路流转 | 漏斗图 | `t_contract`/`t_detection_task`/`t_detection_result`/`t_report` |
| C6 | 检测结果与超标预警 | 明细表 | `t_detection_result` |
| C7 | 系统预警消息 | 明细表 | `t_alert` |
| C8 | 经营与质量概览 | 双仪表盘 | `t_contract_txn`/`t_qc_activity` |
| C9 | 仪器设备状态 | 统计块+饼图 | `t_instrument` |
| C10 | 合同到期预警 | 明细表（按到期排序） | `t_contract` |
| C11 | 实时动态滚动条 | 文字滚动 | 聚合消息 |
| C12 | 数据刷新控制 | 手动/自动刷新 | — |

### 页面布局（对应真实 HTML 的 DOM 结构）

```
.dashboard (100vw×100vh, 深色渐变背景 #0a1628)
├── .header (标题 + 实时时钟 + 实时同步标识)
├── .kpi-row (grid 6列)
│   └── .kpi ×6 (label / value / unit / sub)
├── .main (grid 3列: 1fr 1.4fr 1fr)
│   ├── .col (左)
│   │   ├── panel 合同金额月度趋势   (C2)
│   │   ├── panel 合同结构分布       (C3)
│   │   └── panel 客户合同金额TOP10  (C4)
│   ├── .col (中)
│   │   ├── panel 业务全链路流转     (C5)
│   │   ├── panel 检测结果与超标预警 (C6)
│   │   └── panel 系统预警消息       (C7)
│   └── .col (右)
│       ├── panel 经营与质量概览     (C8, 双仪表盘)
│       ├── panel 仪器设备状态       (C9)
│       └── panel 合同到期预警       (C10)
└── .ticker (实时动态滚动条, C11)
```

> 路径约定：新增页面 `flow-web/src/views/dashboard/executive/index.vue`，路由 `/dashboard/executive`；新增 API 封装 `flow-web/src/api/executive.js`。

---

## 功能需求详述（可被模型直接编码）

> 统一接口约定：前端 `baseURL:/api/v1`，响应体固定为 `{ code:200, data: <T>, message:"" }`（见 `flow-web/src/api/request.js`）。
> 所有图表使用 `echarts@5`（驾驶舱 HTML 已通过 CDN 引入 `echarts@5.4.3`，前端项目需 `npm i echarts@5` 并在组件内 `import * as echarts from 'echarts'`）。

### C1 · KPI 指标卡

| 卡片 | label | value | unit | sub（副指标） | 数据来源 |
|------|-------|-------|------|--------------|----------|
| 1 | 合同总数 | 24 | 份 | 执行中15 / 已完结4（另含草稿4、作废1） | `t_contract` status 计数 |
| 2 | 合同总金额 | 1155.5 | 万元 | 已回款249.5万 / 回款率21.6% | `t_contract.amount` SUM；`t_contract_txn` 收款 SUM |
| 3 | 检测任务 | 10 | 个 | 录入中6 / 已复核3（另含已提交1） | `t_detection_task` status 计数 |
| 4 | 已出报告 | 5 | 份 | 已发布2 / 待审核3 | `t_report` status 计数 |
| 5 | 超标/预警 | 3 | 项 | 检测超标1 / 系统预警2 | `t_detection_result`(超标) + `t_alert`(未处理) |
| 6 | 服务客户 | 100 | 家 | 覆盖15+城市 / 重点10 | `t_customer` 计数 |

**后端接口**：`GET /api/v1/executive/kpi` → 返回：
```json
{
  "contractTotal": 24, "contractExecuting": 15, "contractFinished": 4, "contractDraft": 4, "contractVoid": 1,
  "contractAmountWan": 1155.5, "receivedWan": 249.5, "receiptRate": 21.6,
  "detectionTotal": 10, "detectionEntering": 6, "detectionReviewed": 3, "detectionSubmitted": 1,
  "reportTotal": 5, "reportPublished": 2, "reportPending": 3,
  "overrunCount": 1, "alertCount": 2,
  "customerTotal": 100, "customerCities": 15, "customerKey": 10
}
```

### C2 · 合同金额月度趋势

- 图表类型：`bar`(签订额) + `line`(回款额) 组合，`tooltip:{trigger:'axis'}`。
- X 轴：月份 `['5月','6月','7月','8月']`；Y 轴：万元。
- 真实数据（签订额 / 回款额，单位万元）：
  - 5月：170 / 0
  - 6月：288 / 0
  - 7月：460.4 / 142.5
  - 8月：237.1 / 107
- 签订额来自 `t_contract.sign_date` 按月 SUM(amount)；回款额来自 `t_contract_txn` 收款按月 SUM(amount)。

**后端接口**：`GET /api/v1/executive/contract/monthly-trend` →
```json
{ "months":["5月","6月","7月","8月"], "signAmount":[170,288,460.4,237.1], "receiveAmount":[0,0,142.5,107] }
```

### C3 · 合同结构分布

- 图表类型：`pie`，环形 `radius:['38%','65%']`。
- 数据（value 为份数）：执行中15(蓝#4aa3ff) / 已完结4(绿#3ddc84) / 草稿4(橙#ffb547) / 已作废1(红#ff7878)。
- 来源：`t_contract` 按 `status` 分组计数。`status` 取值见 C10 枚举。

**后端接口**：`GET /api/v1/executive/contract/status-dist` →
```json
{ "data":[ {"name":"执行中","value":15},{"name":"已完结","value":4},{"name":"草稿","value":4},{"name":"已作废","value":1} ] }
```

### C4 · 客户合同金额 TOP10

- 图表类型：`bar` 横向（`yAxis:{type:'category',inverse:true}`），`label` 显示在右侧。
- 数据（客户名 / 金额万元，已按金额降序）：
  | 客户 | 金额(万) |
  |------|---------|
  | 华测生态科技 | 347 |
  | 锦江生态环境分局 | 120 |
  | 恒大化工集团 | 116 |
  | 中环水务有限公司 | 102 |
  | 天青环保科技 | 69 |
  | 蓝天环保科技 | 64 |
  | 金沙江水泥 | 56 |
  | 绿源环境检测 | 54.8 |
  | 明城县环保局 | 52 |
  | 宏宇建筑安装 | 45 |
- 来源：`t_contract` 按 `counterparty_name` 分组 SUM(amount)，降序取前 10。

**后端接口**：`GET /api/v1/executive/contract/top-customers?limit=10` →
```json
{ "data":[ {"name":"华测生态科技","amount":347}, ... ] }
```

### C5 · 业务全链路流转（漏斗）

- 图表类型：自定义 `.funnel`（非 echarts funnel，用 HTML 条形模拟；宽度百分比 = value/24×100%）。
- 数据（name / value / 转化率%）：
  | 环节 | value | 占合同% |
  |------|-------|---------|
  | 合同签订 | 24 | 100% |
  | 检测任务 | 10 | 41.7% |
  | 检测结果 | 8 | 33.3% |
  | 报告生成 | 5 | 20.8% |
  | 报告发布 | 2 | 8.3% |
- 来源：合同签订=`t_contract` 总数；检测任务=`t_detection_task` 总数；检测结果=`t_detection_result` 总数；报告生成=`t_report` 总数；报告发布=`t_report` 中 status=已发布 数。

**后端接口**：`GET /api/v1/executive/funnel` →
```json
{ "data":[
  {"name":"合同签订","value":24,"rate":100},
  {"name":"检测任务","value":10,"rate":41.7},
  {"name":"检测结果","value":8,"rate":33.3},
  {"name":"报告生成","value":5,"rate":20.8},
  {"name":"报告发布","value":2,"rate":8.3}
]}
```

### C6 · 检测结果与超标预警（明细表）

- 表格列：`任务编号 / 样品·客户 / 检测项目 / 实测值 / 标准限值 / 结论`。
- 真实数据（8 条，`t_detection_result`）：

| 任务编号 | 样品·客户 | 检测项目 | 实测值 | 标准限值 | 结论 |
|----------|-----------|----------|--------|----------|------|
| DT202608130002 | 华测生态-COD | 化学需氧量(COD) | 23 mg/L | 45 mg/L | 超标(红) |
| DT202608130001 | 华测生态-COD | 化学需氧量(COD) | 100 mg/L | — | 待判定(灰) |
| DT202608130003 | 中节能-废气 | 苯系物 | 4 mg/m³ | 按标准执行 | 达标(绿) |
| DT202608130003 | 中节能-废气 | 总悬浮颗粒物(TSP) | 4 mg/m³ | 1.0 mg/m³ | 达标(绿) |
| DT202608130004 | 中节能-废水 | 石油类 | 2 mg/L | <7 mg/L | 达标(绿) |
| DT202608130004 | 中节能-废水 | 总氮 | 18 mg/L | <27 mg/L | 达标(绿) |
| DT202608130004 | 中节能-废水 | 总磷 | 0.6 mg/L | <0.7 mg/L | 达标(绿) |
| DT202608130004 | 中节能-废水 | 氨氮 | 3 mg/L | <7 mg/L | 达标(绿) |

- 结论枚举：`达标`(tag-green) / `超标`(tag-red) / `待判定`(tag-gray)。
- 来源：`t_detection_result`（关联 `t_detection_task` 取任务编号、样品/客户、检测项目；实测值、标准限值、结论字段）。

**后端接口**：`GET /api/v1/executive/detection/results?page=1&size=50` →
```json
{ "list":[
  {"taskNo":"DT202608130002","sampleCustomer":"华测生态-COD","item":"化学需氧量(COD)","measured":"23 mg/L","limit":"45 mg/L","conclusion":"超标"},
  ...
], "total":8 }
```

### C7 · 系统预警消息（明细表）

- 表格列：`预警类型 / 级别 / 消息 / 状态`。
- 真实数据（2 条，`t_alert`，均未处理）：

| 预警类型 | 级别 | 消息 | 状态 |
|----------|------|------|------|
| OVERRUN 超标 | HIGH(红) | 检测结果超标预警，请及时复核并通知客户 | 未处理(橙) |
| OVERRUN 超标 | HIGH(红) | 检测结果超标预警，请及时复核并通知客户 | 未处理(橙) |

- 级别枚举：`HIGH`(tag-red) / `MIDDLE`(tag-orange) / `LOW`(tag-blue)；状态：`未处理`(tag-orange) / `已处理`(tag-green)。
- 来源：`t_alert` 按 `status='未处理'` 过滤（或全量，前端标记）。

**后端接口**：`GET /api/v1/executive/alerts?status=未处理` →
```json
{ "list":[
  {"type":"OVERRUN","level":"HIGH","message":"检测结果超标预警，请及时复核并通知客户","status":"未处理"},
  ...
], "total":2 }
```

### C8 · 经营与质量概览（双仪表盘）

- 图表类型：两个 `gauge`，`max:100`，`formatter:'{value}%'`，无指针。
- 仪表1：合同回款率 = **21.6%**（色 #ffd166）；来源 `receivedWan / contractAmountWan ×100`。
- 仪表2：质控合格率 = **66.7%**（色 #3ddc84）；来源 `t_qc_activity` 中合格数/总数（3 条中 2 条合格）。

**后端接口**：复用 `GET /api/v1/executive/kpi` 的 `receiptRate`；新增 `GET /api/v1/executive/quality/qc-rate` → `{ "rate":66.7, "qualified":2, "total":3 }`。

### C9 · 仪器设备状态

- 上方统计块（4 格）：在用63(绿) / 报废1(红) / 维修中0(蓝) / 校准到期0(橙)。
- 下方饼图：`pie` 环形，在用63(绿) / 报废1(红)。
- 来源：`t_instrument` 按 `status` 分组计数。`status` 取值：在用/报废/维修中/校准到期。

**后端接口**：`GET /api/v1/executive/instrument/status` →
```json
{ "stats":{"inUse":63,"scrapped":1,"repairing":0,"calibDue":0},
  "data":[{"name":"在用","value":63},{"name":"报废","value":1}] }
```

### C10 · 合同到期预警（明细表，按到期升序）

- 表格列：`合同编号 / 对方单位 / 金额 / 到期日 / 状态(剩余天数标签)`。
- 数据来源：`t_contract`，筛选 `status='执行中'`（或含已签订未完结），按 `expire_date` 升序。
- 状态标签规则：`剩余天数 = DATEDIFF(expire_date, CURDATE())`；`<0` 逾期(红) / `0~30` 红(紧急) / `31~60` 橙 / `61~120` 蓝 / `>120` 灰(长期)。
- 真实数据（节选，完整 16 条由接口返回）：

| 合同编号 | 对方单位 | 金额(万) | 到期日 | 剩余 |
|----------|----------|---------|--------|------|
| HT202608110002 | 华测生态科技有限公司 | 200 | 08-26 | 12天(红) |
| HT202608130004 | 华测生态科技有限公司 | 26 | 09-30 | 47天(橙) |
| HT202608130019 | 恒大化工集团有限公司 | 18 | 09-30 | 47天(橙) |
| HT202608130002 | 绿源环境检测有限公司 | 45 | 10-31 | 78天(蓝) |
| HT202608130010 | 锦江生态环境分局 | 120 | 2027-06-30 | 长期(灰) |

- 合同 `status` 枚举：草稿 / 执行中 / 已完结 / 已作废。

**后端接口**：`GET /api/v1/executive/contract/expiring?days=180` →
```json
{ "list":[
  {"contractNo":"HT202608110002","counterparty":"华测生态科技有限公司","amountWan":200,"expireDate":"2026-08-26","remainDays":12},
  ...
], "total":16 }
```

### C11 · 实时动态滚动条

- 内容：由后端聚合的近期动态消息（合同到期、回款、检测、预警、报告、到期等），前端用 CSS `animation:scroll` 无限滚动（轨道内容复制一份实现无缝）。
- 真实示例文案（来自驾驶舱 HTML `.ticker-track`）：
  - 【合同】HT202608110002 华测生态科技 200万合同将于 08-26 到期，请商务跟进续签
  - 【回款】收到合同 HT202608130001 回款 50万元，经办人：公孙胜
  - 【检测】DT202608140005~010 共6个检测任务正在录入中，涉及 pH/COD/氨氮
  - 【预警】检测结果 COD 超标预警（HIGH），请质量负责人及时复核
  - 【报告】HJ-JC-2026-0002 终验报告已发布，含1项超标项
  - 【报告】HJ-JC-2026-0003/004/005 共3份报告待审核
  - 【合同】本月新签合同 237.1万元，环比下降 48.5%
  - 【仪器】64台仪器设备中63台在用，1台已报废

**后端接口**：`GET /api/v1/executive/ticker` → `{ "list":[ {"type":"合同","text":"..."}, ... ] }`

### C12 · 数据刷新控制

- 顶部右上角显示实时时钟（前端 `setInterval` 每秒更新，格式 `YYYY-MM-DD HH:mm:ss 周X`）。
- 「实时同步」标识：默认开启自动刷新，间隔 **60 秒**轮询所有聚合接口（可配置）。
- 提供「刷新」按钮手动触发一次全量拉取。
- 断线/后端未启动时静默失败（参考现有 dashboard 的 `catch` 忽略模式）。

---

## 数据结构与口径（关键字段映射）

> 以下表名为现有系统真实表（见 `flow-engine/src/main/resources/db/schema_mysql.sql`），接口 SQL 直接基于这些表聚合。

| 指标 | 表 | 字段/聚合 |
|------|----|-----------|
| 合同总数/状态 | `t_contract` | `COUNT(*)`；`status` 分组 |
| 合同金额 | `t_contract` | `SUM(amount)`（单位：万元） |
| 回款额/回款率 | `t_contract_txn` | `type='收款'` 时 `SUM(amount)`；回款率=收款SUM/合同金额SUM |
| 月度趋势 | `t_contract` | `DATE_FORMAT(sign_date,'%m')` 分组 SUM(amount) |
| 客户TOP | `t_contract` | `counterparty_name` 分组 SUM(amount) |
| 检测任务 | `t_detection_task` | `COUNT(*)`；`status` 分组 |
| 检测结果 | `t_detection_result` | `COUNT(*)`；`conclusion` 分组 |
| 报告 | `t_report` | `COUNT(*)`；`status` 分组 |
| 系统预警 | `t_alert` | `status='未处理'` 计数；`level` 分组 |
| 仪器设备 | `t_instrument` | `status` 分组（在用/报废/维修中/校准到期） |
| 质控合格率 | `t_qc_activity` | 合格数/总数 |
| 服务客户 | `t_customer` | `COUNT(*)`；去重城市数 |

**合同 status 枚举（与 C3/C10 一致）**：`草稿` / `执行中` / `已完结` / `已作废`
**检测任务 status 枚举**：`待录入` / `录入中` / `已提交` / `已退回` / `已复核`
**报告 status 枚举**：`待审核` / `已发布`
**检测结果 conclusion 枚举**：`达标` / `超标` / `待判定`
**仪器 status 枚举**：`在用` / `报废` / `维修中` / `校准到期`
**预警 level 枚举**：`HIGH` / `MIDDLE` / `LOW`；**预警 status 枚举**：`未处理` / `已处理`

---

## 后端接口总表（新增 Controller 建议：`ExecutiveController`）

| 方法 | 路径 | 说明 | 返回 |
|------|------|------|------|
| GET | `/api/v1/executive/kpi` | C1 全部 KPI | 见 C1 |
| GET | `/api/v1/executive/contract/monthly-trend` | C2 月度趋势 | months/signAmount/receiveAmount |
| GET | `/api/v1/executive/contract/status-dist` | C3 状态分布 | data[] |
| GET | `/api/v1/executive/contract/top-customers` | C4 客户TOP10 | data[] (name/amount) |
| GET | `/api/v1/executive/funnel` | C5 业务漏斗 | data[] |
| GET | `/api/v1/executive/detection/results` | C6 检测结果 | list/total |
| GET | `/api/v1/executive/alerts` | C7 预警 | list/total |
| GET | `/api/v1/executive/quality/qc-rate` | C8 质控率 | rate/qualified/total |
| GET | `/api/v1/executive/instrument/status` | C9 仪器 | stats/data[] |
| GET | `/api/v1/executive/contract/expiring` | C10 到期预警 | list/total |
| GET | `/api/v1/executive/ticker` | C11 滚动动态 | list[] |

> 所有接口返回统一 `{ code:200, data, message:"" }`；建议后端对聚合查询加 `@Cacheable`（Caffeine，参考现有 `config`）以减轻大屏轮询压力。

---

## 前端实现要点（可直接编码）

1. **路由**：在 `flow-web/src/router` 增加 `{ path:'/dashboard/executive', component:()=>import('@/views/dashboard/executive/index.vue'), meta:{title:'高层数据驾驶舱'} }`。
2. **API 封装**：新建 `flow-web/src/api/executive.js`，导出上述 11 个 `request.get(...)` 函数，返回 `res.data`（响应拦截器已剥离 `{code,data,message}`）。
3. **组件结构**：`executive/index.vue` 内含：
   - `useExecutiveData()` composable：并行 `Promise.all` 拉取 11 个接口，60s 定时刷新。
   - 各面板为独立子组件或就地渲染（首版可单文件内联，后续拆分 `components/KpiCard.vue`、`components/ContractTrend.vue` 等）。
4. **图表**：`import * as echarts from 'echarts'`；每个 `.chart` 容器 `ref` 后 `echarts.init(el).setOption(...)`；`window.resize` 时遍历 `echarts.getInstanceByDom(el).resize()`（与驾驶舱 HTML 一致）。
5. **样式**：直接复用驾驶舱 HTML 的 CSS（深色渐变 `.dashboard`、`.kpi`、`.panel`、`.tbl`、`.tag-*`、`.funnel-*`、`.ticker` 等），迁移为 `<style scoped>` 或全局 dark 主题类。
6. **配色**：KPI 蓝 `#4aa3ff`、金额金 `#ffd166`、达标绿 `#3ddc84`、超标红 `#ff7878`、预警橙 `#ffb547`；背景 `#0a1628`。
7. **空/错处理**：接口失败 `catch` 静默，保留上一次数据（与现有 dashboard 一致）。

---

## 非功能性需求

1. **性能**：驾驶舱为聚合查询，单接口响应 < 500ms；全量 11 接口并行拉取 < 1s；自动刷新间隔 60s，避免压垮数据库。
2. **兼容性**：Chrome / Edge 最新版；分辨率 ≥ 1920×1080（大屏），向下兼容 1366×768。
3. **可访问性**：深色背景下文字对比度 ≥ 4.5:1；关键超标/预警用红橙高亮。
4. **安全性**：驾驶舱为经营敏感数据，需登录态（`Authorization: Bearer <token>`，现有拦截器已注入）；建议后端接口增加高管角色校验（如 `ROLE_EXECUTIVE` 或经营/质量负责人角色）。
5. **可维护性**：所有指标口径集中在一个 SQL Mapper（`ExecutiveMapper.xml`）与一份 PRD 字段映射表，口径变更只改一处。

---

## 验收标准

1. 页面在 1920×1080 下完整渲染 6 KPI + 11 面板 + 滚动条，无溢出/错位；
2. 所有数值与 CSV/HTML 真实数据一致（合同24、金额1155.5万、回款率21.6%、检测10、报告5、超标预警3、客户100、仪器64）；
3. 开启自动刷新后每 60s 数据更新；手动刷新即时生效；
4. 后端 11 个接口均返回统一 `{code:200,data}` 结构；
5. 接口异常时页面不白屏，保留历史数据。
```
```

> 注：本 PRD 的"真实数据"为 2026-08-14 数据快照（见随附 `表格_20260814.csv` 与 `LIMS高层领导数据驾驶舱（真实数据版）.html`），研发实现时接口应基于表聚合动态计算，而非硬编码上述数值。
