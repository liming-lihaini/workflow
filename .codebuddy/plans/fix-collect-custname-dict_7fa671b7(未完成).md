---
name: fix-collect-custname-dict
overview: 修复收样工作台"手动收集样品"的两个问题：(1) 单位信息应依据委托单自动获取，去掉手选下拉改为只读；(2) 固定剂/质控方式数据字典加载失败时给出真实错误并健壮解析，确保加载成功。
todos:
  - id: fix-unit-info-readonly
    content: 将单位信息下拉改为只读展示，绑定 form.custName
    status: pending
  - id: fix-dispatch-custname
    content: onDispatchChange 中删除 custOptions 重选逻辑，自动用 raw.custName 带出单位
    status: pending
    dependencies:
      - fix-unit-info-readonly
  - id: harden-dict-load
    content: loadDicts 独立解包字典结果并打印真实错误，空结果不误报
    status: pending
  - id: verify-collect-flow
    content: 本地启动验证抽屉级联、字典加载与单位自动获取
    status: pending
    dependencies:
      - fix-unit-info-readonly
      - fix-dispatch-custname
      - harden-dict-load
---

## 用户需求

修复收样工作台"手动收集样品"抽屉中已暴露的两个问题：

## 核心问题

- 问题一：记录采样信息时，固定剂和质控方式的数据字典加载失败。字典类型（sample_preservative、moni_qc_type）已在后端正确播种且状态正常，后端接口与前端调用路径均正确，真实失败原因被前端 catch 吞掉且无错误打印，需让加载逻辑健壮并打印真实错误。
- 问题二：单位信息应依据委托单自动获取，而非让用户手动从下拉框选择。当前实现把单位信息做成可手选的下拉框，违背需求；选择派单后应自动从委托单（派单列表已返回的 custName）带出单位并只读展示。

## 功能内容

- 单位信息字段改为只读展示，选择采样派单后自动带出委托单所属单位名称。
- 固定剂、质控方式字典下拉加载失败时给出明确错误提示并打印真实异常，便于定位；空结果不再误报失败。
- 现场采样照片、采样参数动态表单、留样开关等既有功能保持不变。

## 技术栈

- 前端框架：Vue 3（`<script setup>` + Composition API）
- UI 组件库：ant-design-vue 4.x（a-drawer / a-select / a-form-item / a-input）
- 请求封装：axios 实例（baseURL `/api/v1`，响应拦截器返回 `res` 即 Result 对象，`res.data` 为业务数据）
- 后端接口（已验证正确，无需改动）：`GET /api/v1/system/dict/items/code/{dictCode}`、`GET /api/v1/ems/base/entrusts/{id}`、`GET /api/v1/ems/base/sampling-orders/dispatch-list`

## 实现方案

本次仅修改前端 `ReceiveWorkbench.vue`，后端与 api/ems.js 均无需改动（经验证接口与字段映射正确）。

### 关键决策与理由

1. **单位信息改为只读**：派单接口 `listDispatchBoard` 已返回 `custName`（来自委托单 EmsEntrust 单位字段），`dispatchOptions` 的 `raw.custName` 已可用。选择派单后直接将 `raw.custName` 赋给 `form.custName`，模板用只读展示，避免用户误选或选不到正确单位。删除 `custOptions` 下拉重选逻辑既符合需求，也减少一个不必要的数据源与状态。
2. **保留 `getEntrust` 调用**：其仅用于加载委托单点位（`points`），与单位信息自动获取解耦，逻辑保留。
3. **字典加载健壮性**：原 `loadDicts` 用 `Promise.all` 包裹两个字典请求，任一 reject 即整体进入 catch 并吞掉错误。改为对两个字典分别独立 try/catch 解包 `res.data`（List），空列表仅 `console.warn` 不报失败；catch 中 `console.error` 打印真实异常并保留 `message.error` 用户提示。这样既能定位真实错误（如接口、权限、字段），也不会因单字典异常阻断另一个。

### 性能与可靠性

- 字典与选项均为小体量数据（个位数到数十项），无分页/N+1 风险；独立 try/catch 避免互相阻塞。
- 单位信息直接来自已加载的派单列表 `raw.custName`，无额外请求开销。

## 实现注意事项

- 严格复用现有 `request` 拦截器约定：`getDictItems(...)` 返回 `res`（Result），业务列表在 `res.data`，字段为 `itemValue`/`itemText`。
- 仅删除 `custOptions` 的写入逻辑，不删除 `custOptions` 声明以外的无关代码，控制改动半径。
- 保持 `onDispatchChange` 中对点位（`getEntrust`）的加载不被破坏。

## 架构与目录结构

本次为单文件局部修改，不新增文件，不调整架构。

```
flow-web/src/views/ems/base/
└── ReceiveWorkbench.vue   # [MODIFY] 单位信息改只读、onDispatchChange 自动带出 custName、load 字典健壮化
```

## 关键代码改动点（描述，非实现体）

- 模板 108-116 行：将 `<a-form-item label="单位信息">` 内的 `<a-select :options="custOptions">` 替换为只读展示（如 `<a-input :value="form.custName || '—'" disabled />` 或文本 span），与"委托单"字段展示风格一致。
- `onDispatchChange`（约 392-431 行）：删除第 408-411、424-426 行将 custName 塞入 `custOptions` 并重选的逻辑；改为 `form.custName = raw.custName && raw.custName !== '—' ? raw.custName : ''`。
- `loadDicts`（约 379-390 行）：拆分为两个独立请求，各自 `await getDictItems(...)` 后取 `res.data` 映射；空结果 `console.warn`；`catch` 中 `console.error(e)` 并打印接口信息，仍 `message.error('加载数据字典失败：' + 具体类型)`。

## 验证方式

- 启动前端 dev server（端口 3000），打开收样工作台 → 点击"手动收集样品"。
- 选择"已派单"派单后：单位信息自动显示委托单单位（只读）；选择检测项目后采样参数表单加载；固定剂、质控方式下拉有数据且不再报"加载数据字典失败"。
- 打开浏览器控制台确认无未捕获字典加载异常；若后端字典接口异常，`console.error` 能打印真实错误。