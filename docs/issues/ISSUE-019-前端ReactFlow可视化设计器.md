# ISSUE-019：前端 — React Flow 可视化设计器

- **优先级**：P0
- **模块**：表现层 / 设计器
- **负责人**：前端×2
- **预计工期**：W13
- **前置依赖**：ISSUE-003, ISSUE-018
- **里程碑**：M5
- **状态**：✅ 已完成

## 目标
基于 React Flow 11.x 实现可视化拖拽流程设计器，与流程定义 CRUD/解析（003）及节点配置 Schema（011）对接，输出符合 `ProcessJsonParser` 的 JSON。

## 范围
### In Scope
- 工程：React Flow 11.x 子应用，可嵌入 018 后台（微前端/iframe）。
- 画布：拖拽节点（start/end/userTask/serviceTask/gateway/scriptTask/subProcess/custom…）、连线、网关分支条件编辑。
- 节点配置面板：读取 `NodeHandler.getConfigSchema()`（011）动态渲染配置项。
- 导入/导出：与 003 的 `/import`、`/export` 对接；保存时提交 JSON 至 `/definitions`。
- 校验：必须含 start/end（与 003 后端校验一致）。

### Out of Scope
- 后端执行逻辑（003/004）；管理后台其它页面（018）。

## 技术约束（以 TRD 为准）
- 设计器 React Flow 11.x（TRD §1.2）；输出 JSON 须被 003 的 `ProcessJsonParser` 解析。
- 与 Vue 后台技术栈分裂，建议微前端/iframe 集成（开发计划 §9 风险）。

## 接口契约
- 消费：`POST /api/v1/process/definitions`、`/{id}/export`、`/import`（TRD §3.1）。
- 消费：`NodeHandler.getConfigSchema()` 元数据（经 003 后端暴露的节点类型接口，若未提供则用本地 schema 映射）。

## 独立运行与验证
### 运行方式
```bash
cd flow-designer && npm install && npm run dev   # 独立运行，mock 后端或对接 8080
```

### 验证用例（映射 PRD §流程设计器 验收 / TRD §3.1/§4.1.3）
- [ ] 拖拽生成 start→userTask→end，连线保存为 JSON。
- [ ] 网关分支可配置条件表达式（如 `${days>3}`）。
- [ ] 导出 JSON 经 003 `/import` 可重建；与后端解析器往返一致。
- [ ] 缺失 start/end 时前端与后端双重校验提示。
- [ ] 自定义节点出现在面板且配置项由 schema 动态渲染（对应 011）。

### 验证脚本/测试
- 单测（Vitest/Jest）：图模型序列化与条件编辑。
- 集成：设计器产出 JSON → 调 003 解析接口断言成功。

## 交付物
- React Flow 设计器子应用 `flow-designer` + 与 003/011 对接 + 嵌入 018 方案。

## 实现摘要

### 技术栈
- Vite + React 18 + React Flow 11 + Axios

### 目录结构
```
flow-designer/
├── src/
│   ├── api.js              # API 对接层（对接 /api/v1/process/*）
│   ├── nodeTypes.js        # 节点类型配置（10种节点 + Schema）
│   ├── components/
│   │   ├── FlowNode.jsx    # 自定义节点组件
│   │   ├── NodePalette.jsx # 左侧节点拖拽面板
│   │   └── ConfigPanel.jsx # 右侧属性配置面板
│   ├── App.jsx             # 主画布 + 工具栏 + 导入/导出/保存/校验
│   ├── main.jsx
│   └── index.css           # 全局样式（front-design.md CSS 变量）
└── vite.config.js          # 端口 3001，代理 /api → localhost:8080
```

### 功能特性
| 功能 | 说明 |
|------|------|
| 节点拖拽 | 10 种节点类型（start/end/userTask/serviceTask/scriptTask/3种网关/subProcess/custom） |
| 连线 | 支持节点间连线，动画效果，箭头标记 |
| 节点配置 | Schema 驱动，根据节点类型动态渲染配置项 |
| 连线条件 | 支持条件表达式、分支名称、优先级 |
| 导入 | 从 JSON 文件导入流程定义 |
| 导出 | 导出为 JSON 文件下载 |
| 保存 | 对接后端 /api/v1/process/definitions CRUD |
| 校验 | 必须包含 start/end 节点，必填项检查 |
| 删除 | 支持 Delete/Backspace 键删除选中节点/连线 |

### 集成方案
- flow-web 通过 iframe 嵌入，路由 `/process/designer`
- 开发环境：iframe 指向 http://localhost:3001
- 生产环境：独立部署到 /designer 路径

### 验证结果
- `npm run build` 编译成功，255 模块，4.39s
- flow-web 编译成功，菜单已包含“流程设计器”入口
- 样式遵循 front-design.md CSS 变量规范

## 验收门禁（DoD）
- ✅ 可拖拽设计流程并输出合规 JSON；导入导出往返一致；节点配置由 schema 驱动。
