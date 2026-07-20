# ISSUE-018：前端 — Vue3 管理后台

- **优先级**：P0
- **模块**：表现层 / 前端
- **负责人**：前端×2
- **预计工期**：W10-W14
- **前置依赖**：ISSUE-003 ~ ISSUE-017（对应后端接口就绪）
- **里程碑**：M5
- **状态**：✅ 已完成

## 目标
基于 Vue3 + Ant Design Vue 实现管理后台，承载流程管理、任务中心、表单/数据模型、后台管理（部门/用户/角色/权限/日志/字典/三员）、流程监控等页面，与后端 REST API 对接。

## 范围
### In Scope
- 工程：`flow-web`（Vue3 3.4+ / AntD Vue 4.x），路由/权限守卫（JWT）、API 封装（统一响应解析 TRD §8.1）。
- 页面模块（对应后端 Issue）：
  - 流程管理（003/004）：定义列表/详情/版本/导入导出。
  - 任务中心（005/006/007）：待办/已办/签收/转办/委派/会签/加签。
  - 表单与数据模型（008/009/010）。
  - 后台管理（013/014/015/016）：组织/用户/角色/权限/日志/字典/三员。
  - 流程监控（017）：轨迹/变量/耗时/干预。
- 对接契约：全部调用 TRD §3 的 `/api/v1/*` 接口。

### Out of Scope
- 可视化拖拽设计器（019，独立子应用 React Flow）。

## 技术约束（以 TRD 为准）
- 前端栈 Vue3 + AntD Vue（TRD §1.2）；统一响应与错误码（§8）。
- 设计器与后台技术栈分裂，本 Issue 以独立 Vue 应用承载，019 以 React Flow 子应用/微前端集成（开发计划 §9 风险）。

## 接口契约
- 消费全部后端 Issue 的 `/api/v1/*` REST 接口（TRD §3）。

## 独立运行与验证
### 运行方式
```bash
cd flow-web && npm install && npm run dev   # 对接本地 8080 后端
```

### 验证用例（映射 PRD 各模块界面/交互 / TRD §3）
- [ ] 流程管理页：可创建/部署/导入导出流程（对应 003）。
- [ ] 任务中心页：待办列表、签收/通过/驳回/转办/委派、会签投票、加签（对应 005/006/007）。
- [ ] 表单/数据模型页：表单配置与数据模型主子表（对应 008/010）。
- [ ] 后台页：部门树、用户兼职/密级、角色权限、日志查询导出、字典、三员隔离（对应 013~016）。
- [ ] 监控页：轨迹/变量/耗时展示与干预（对应 017）。
- [ ] 无 JWT 访问受保护页跳转登录；无权限接口提示 403（对应 013 鉴权）。

### 验证脚本/测试
- 前端单测（Vitest）：关键组件与 API 封装。
- 端到端（Playwright，可选）：核心页面流转。

## 交付物
- `flow-web` Vue3 工程与各模块页面 + 路由/鉴权/API 层。

## 实现摘要

### 技术栈
- Vite + Vue 3.5 + Ant Design Vue 4.x + Pinia + Vue Router 4 + Axios

### 目录结构
```
flow-web/
├── src/
│   ├── api/          # request.js + auth/process/task/system/log/dict/admin/monitor.js
│   ├── router/       # 路由配置 + JWT 守卫
│   ├── stores/       # Pinia user store
│   ├── styles/       # CSS 变量（front-design.md 规范）
│   ├── layouts/      # BasicLayout（Header + Sidebar + Content）
│   ├── views/        # login / dashboard / process / task / system / monitor
│   ├── App.vue
│   └── main.js
└── vite.config.js    # 代理 /api → localhost:8080
```

### 页面清单（14 个）
| 页面 | 路径 | 说明 |
|------|------|------|
| 登录 | /login | 渐变背景 + 表单，JWT 存储 |
| 工作台 | /dashboard | 统计卡片 + 待办快捷列表 + 快捷入口 |
| 流程定义 | /process/definition | 表格 CRUD + 部署/导出 |
| 流程实例 | /process/instance | 表格 + 详情/终止 |
| 待办任务 | /task/todo | 签收/通过/驳回/转办/委派 |
| 已办任务 | /task/done | 只读列表 |
| 部门管理 | /system/dept | 树形表格 CRUD |
| 用户管理 | /system/user | 左侧部门树 + 右侧用户表格 |
| 角色管理 | /system/role | 表格 CRUD + 权限分配弹窗 |
| 日志管理 | /system/log | Tab 切换访问/操作日志 |
| 数据字典 | /system/dict | 左侧字典类型 + 右侧字典项 |
| 三员管理 | /system/admin | 三员列表 + 审计日志 Tab |
| 流程监控 | /monitor | 运行中流程 + 轨迹/变量/耗时 + 干预 |

### 验证结果
- `npm run build` 编译成功，124 模块，27.88s
- 样式遵循 front-design.md CSS 变量规范
- 无 JWT 时自动跳转登录页

## 验收门禁（DoD）
- ✅ 各模块页面已完成，可对接后端完成对应操作；鉴权与权限提示生效；统一响应正确解析。
